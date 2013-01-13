package base;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.Team;
import battlecode.common.Upgrade;

public class NavSystem {
	
	public static BaseRobot robot;
	public static RobotController rc;
	public static int[] directionOffsets;
	
	public static MapLocation currentWaypoint;
	
	public static NavMode navMode = NavMode.NEUTRAL;
	public static MapLocation destination;
	
	public static MapLocation HQLocation;
	public static MapLocation enemyHQLocation;

	public static int mapHeight;
	public static int mapWidth;
	public static MapLocation mapCenter;
	
	/**
	 * MUST CALL THIS METHOD BEFORE USING NavSystem
	 * @param myRC
	 */
	public static void init(BaseRobot myRobot) {
		robot = myRobot;
		rc = robot.rc;
		int robotID = rc.getRobot().getID();
		
		// Randomly assign soldiers priorities for trying to move left or right
		if (robotID % 4 == 0 || robotID % 4 == 1) {
			directionOffsets = new int[]{0,1,-1,2,-2};
		} else {
			directionOffsets = new int[]{0, -1,1,-2,2};
		}
		
		// Get locations of our HQ and enemy HQ
		HQLocation = rc.senseHQLocation();
		enemyHQLocation = rc.senseEnemyHQLocation();
		// Get map dimensions
		mapHeight = rc.getMapHeight();
		mapWidth = rc.getMapWidth();
		// Calculate the center of the map
		mapCenter = new MapLocation(mapWidth / 2, mapHeight / 2);
	}
	
	/**
	 * Tells rc to go to a location while defusing mines along the way
	 * @param location
	 * @throws GameActionException
	 */
	public static void goToLocation(MapLocation location) throws GameActionException {
		Direction dir = rc.getLocation().directionTo(location);
		if (dir != Direction.OMNI) {
			goDirectionAndDefuse(dir);
		}
	}
	
	public static void goToLocationAvoidMines(MapLocation location) throws GameActionException {
		Direction dir = rc.getLocation().directionTo(location);
		if (dir != Direction.OMNI){
			goDirectionAvoidMines(dir);
		}
	}
	
	public static void goDirectionAndDefuse(Direction dir) throws GameActionException {
		Direction lookingAtCurrently = dir;
		lookAround: for (int d : directionOffsets) {
			lookingAtCurrently = Direction.values()[(dir.ordinal() + d + 8) % 8];
			if (rc.canMove(lookingAtCurrently)) {
				if (hasBadMine(rc.getLocation().add(lookingAtCurrently))) {
					rc.defuseMine(rc.getLocation().add(lookingAtCurrently));
				} else {
					rc.move(lookingAtCurrently);
				}
				break lookAround;
			}
		}
	}
	
	public static void goDirectionAvoidMines(Direction dir) throws GameActionException {
		Direction lookingAtCurrently = dir;
		boolean movedYet = false;
		lookAround: for (int d : directionOffsets) {
			lookingAtCurrently = Direction.values()[(dir.ordinal() + d + 8) % 8];
			if (rc.canMove(lookingAtCurrently)) {
				if (!hasBadMine(rc.getLocation().add(lookingAtCurrently))) {
					movedYet = true;
					rc.move(lookingAtCurrently);
				}
				break lookAround;
			}
			if (!movedYet) { // if the robot still hasn't moved
				if (rc.senseNearbyGameObjects(Robot.class, 2, rc.getTeam().opponent()).length == 0) {
					// if there are no nearby enemies
					rangedDefuseMine();
				}
			}
		}
	}
	
	public static void rangedDefuseMine() throws GameActionException {
		if (rc.hasUpgrade(Upgrade.DEFUSION)) {
			MapLocation[] mines = rc.senseMineLocations(rc.getLocation(), 14, rc.getTeam().opponent());
			if (mines.length > 0) {
				rc.defuseMine(mines[0]);
			}
		}
	}
	
	private static boolean hasBadMine(MapLocation location) {
		Team bombTeam = rc.senseMine(location);
		return !(bombTeam == null || bombTeam == rc.getTeam());
	}
	
	/**
	 * Follow the waypoint stored in currentWaypoint
	 * @throws GameActionException
	 */
	public static void followWaypoints() throws GameActionException {
		// If we're close to currentWaypoint, find the next one
		if (rc.getLocation().distanceSquaredTo(destination) <= 5) {
			// Stop nav-ing?
			navMode = NavMode.NEUTRAL;
			// We're done following waypoints
			goToLocation(destination);
		} else if (rc.getLocation().distanceSquaredTo(currentWaypoint) <= 5){
			// We're close to currentWaypoint, so find the next one
			switch (navMode) {
			case SMART:
				calculateSmartWaypoint();
				break;
			case BACKDOOR:
				calculateBackdoorWaypoint();
				break;
			default:
				break;
			}
			goToLocation(currentWaypoint);
		} else {
			// Keep moving to the current waypoint
			goToLocation(currentWaypoint);
		}
	}
	
	/**
	 * Sets up the backdoor navigation system for a given endLocation.
	 * @param endLocation
	 * @throws GameActionException
	 */
	public static void setupBackdoorNav(MapLocation endLocation) throws GameActionException {
		navMode = NavMode.BACKDOOR;
		destination = endLocation;
		// TODO: Calculate all the waypoints first and save them (this is different from smart nav)
		calculateBackdoorWaypoint();
	}
	
	/**
	 * Sets up the smart navigation system for a given endLocation.
	 * This means that we will set navMode = navMode.SMART
	 * @param endLocation
	 * @throws GameActionException
	 */
	public static void setupSmartNav(MapLocation endLocation) throws GameActionException {
		navMode = NavMode.SMART;
		destination = endLocation;
		calculateSmartWaypoint();
	}
	
	/**
	 * Gets the next backdoor waypoint that was calculated when setupBackdoorNav() was called.
	 * @throws GameActionException
	 */
	public static void calculateBackdoorWaypoint() throws GameActionException {
		//
	}
	
	/**
	 * Calculates the next smart waypoint to take and writes it to currentWaypoint.
	 * @throws GameActionException
	 */
	public static void calculateSmartWaypoint() throws GameActionException {
		MapLocation currentLocation = rc.getLocation();
		if (currentLocation.distanceSquaredTo(destination) <= Constants.PATH_GO_ALL_IN_SQ_RADIUS) {
			// If we're already really close to the destination, just go straight in
			currentWaypoint = destination;
			return;
		}
		// Otherwise, try to pick a good direction to move in based on mines and direction to destination
		int bestScore = Integer.MAX_VALUE;
		MapLocation bestLocation = null;
		Direction dirLookingAt = currentLocation.directionTo(destination);
		for (int i = -2; i <= 2; i++) {
			Direction dir = Direction.values()[(dirLookingAt.ordinal() + i + 8) % 8];
			MapLocation iterLocation = currentLocation.add(dir, Constants.PATH_OFFSET_RADIUS);
			int currentScore = smartScore(iterLocation, Constants.PATH_CHECK_RADIUS, destination);
			if (currentScore < bestScore) {
				bestScore = currentScore;
				bestLocation = iterLocation;
			}
		}
		currentWaypoint = bestLocation;
	}

	/**
	 * Smart scoring function for calculating how favorable it is to move in a certain direction.
	 * @param location
	 * @param radius
	 */
	public static int smartScore(MapLocation location, int radius, MapLocation endLocation) {
		int numMines = rc.senseNonAlliedMineLocations(location, radius * radius).length;
		// maximum number of mines within this radius should be 3 * radius^2
		int distanceSquared = location.distanceSquaredTo(endLocation);
		int mineDelay;
		if (rc.hasUpgrade(Upgrade.DEFUSION)) {
			mineDelay = GameConstants.MINE_DEFUSE_DEFUSION_DELAY;
		} else {
			mineDelay = GameConstants.MINE_DEFUSE_DELAY;
		}
		return distanceSquared + (int)(0.83 * mineDelay * numMines);
	}
}