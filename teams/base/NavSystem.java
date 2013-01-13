package base;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Upgrade;

public class NavSystem {
	
	public static BaseRobot robot;
	public static RobotController rc;
	public static int[] directionOffsets;
	
	// Used by both smart and backdoor nav
	public static MapLocation currentWaypoint;
	public static NavMode navMode = NavMode.NEUTRAL;
	public static MapLocation destination;
	
	// Used to store the waypoints of a backdoor nav computation
	public static MapLocation[] backdoorWaypoints; // Should always have four MapLocations
	public static int backdoorWaypointsIndex;
	
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
			directionOffsets = new int[]{0,-1,1,-2,2};
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
		if (rc.getLocation().distanceSquaredTo(destination) <= Constants.WAYPOINT_SQUARED_DISTANCE_CHECK) {
			// Stop nav-ing?
			navMode = NavMode.NEUTRAL;
			// We're done following waypoints
			goToLocation(destination);
		} else if (rc.getLocation().distanceSquaredTo(currentWaypoint) <= Constants.WAYPOINT_SQUARED_DISTANCE_CHECK){
			// We're close to currentWaypoint, so find the next one
			switch (navMode) {
			case SMART:
				getSmartWaypoint();
				break;
			case BACKDOOR:
				getBackdoorWaypoint();
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
		// Calculate all the waypoints first and save them (this is different from smart nav)
		MapLocation currentLocation = rc.getLocation();
		Direction dirToEndLocation = currentLocation.directionTo(endLocation);
		// How close are we to the wall?
		int horzDistanceToWall = mapWidth / 2 - Math.abs(endLocation.x - mapCenter.x);
		int vertDistanceToWall = mapHeight / 2 - Math.abs(endLocation.y - mapCenter.y);
		int distanceToWall = (int)(0.8 * Math.min(horzDistanceToWall, vertDistanceToWall));
		MapLocation firstWaypoint = currentLocation.add(dirToEndLocation, -distanceToWall);
		MapLocation lastWaypoint = endLocation.add(dirToEndLocation, distanceToWall);
		backdoorWaypoints = new MapLocation[]{firstWaypoint, null, null, lastWaypoint};
		// Now let's try to find some intermediate waypoints to follow
		// Let's see if we should move horizontally first or vertically first
		int dx = endLocation.x - currentLocation.x;
		int dy = endLocation.y - currentLocation.y;
		if (Math.abs(dx) > Math.abs(dy)) {
			// We're vertically really close, but not horizontally, so move vertically first
			if (Util.Random() < 0.5) {
				// Try moving up, then horizontally, then down to endLocation
				backdoorWaypoints[1] = new MapLocation(currentLocation.x, Constants.BACKDOOR_WALL_BUFFER);
				// We need to know if endLocation is closer to the left wall or the right wall
				if (endLocation.x < mapWidth - endLocation.x) { // left wall
					backdoorWaypoints[2] = new MapLocation(Constants.BACKDOOR_WALL_BUFFER, Constants.BACKDOOR_WALL_BUFFER);
				} else { // right wall
					backdoorWaypoints[2] = new MapLocation(mapWidth - Constants.BACKDOOR_WALL_BUFFER, Constants.BACKDOOR_WALL_BUFFER);
				}
			} else {
				// Try moving down, then horizontally, then up to endLocation
				backdoorWaypoints[1] = new MapLocation(currentLocation.x, mapHeight - Constants.BACKDOOR_WALL_BUFFER);
				// We need to know if endLocation is closer to the left wall or the right wall
				if (endLocation.x < mapWidth - endLocation.x) { // left wall
					backdoorWaypoints[2] = new MapLocation(Constants.BACKDOOR_WALL_BUFFER, mapHeight - Constants.BACKDOOR_WALL_BUFFER);
				} else { // right wall
					backdoorWaypoints[2] = new MapLocation(mapWidth - Constants.BACKDOOR_WALL_BUFFER, mapHeight - Constants.BACKDOOR_WALL_BUFFER);
				}
			}
		} else {
			// We're horizontally really close, but not vertically, so move horizontally first
			if (Util.Random() < 0.5) {
				// Try moving left, then vertically, then right to endLocation
				backdoorWaypoints[1] = new MapLocation(Constants.BACKDOOR_WALL_BUFFER, currentLocation.y);
				// We need to know if endLocation is closer to the top wall or the bottom wall
				if (endLocation.y < mapHeight - endLocation.y) { // top wall
					backdoorWaypoints[2] = new MapLocation(Constants.BACKDOOR_WALL_BUFFER, Constants.BACKDOOR_WALL_BUFFER);
				} else { // bottom wall
					backdoorWaypoints[2] = new MapLocation(Constants.BACKDOOR_WALL_BUFFER, mapHeight - Constants.BACKDOOR_WALL_BUFFER);
				}
			} else {
				// Try moving right, then vertically, then left to endLocation
				backdoorWaypoints[1] = new MapLocation(mapWidth - Constants.BACKDOOR_WALL_BUFFER, currentLocation.y);
				// We need to know if endLocation is closer to the top wall or the bottom wall
				if (endLocation.y < mapHeight - endLocation.y) { // top wall
					backdoorWaypoints[2] = new MapLocation(mapWidth - Constants.BACKDOOR_WALL_BUFFER, Constants.BACKDOOR_WALL_BUFFER);
				} else { // bottom wall
					backdoorWaypoints[2] = new MapLocation(mapWidth - Constants.BACKDOOR_WALL_BUFFER, mapHeight - Constants.BACKDOOR_WALL_BUFFER);
				}
			}
		}
		// Upon calling getBackdoorWaypoint(), backdoorWaypointsIndex will be incremented by 1
		backdoorWaypointsIndex = -1;
		getBackdoorWaypoint();
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
		getSmartWaypoint();
	}
	
	/**
	 * Gets the next backdoor waypoint that was calculated when setupBackdoorNav() was called.
	 * @throws GameActionException
	 */
	public static void getBackdoorWaypoint() throws GameActionException {
		if (backdoorWaypointsIndex == backdoorWaypoints.length - 1){
			// We're at the last waypoint, so just go straight to the destination
			currentWaypoint = destination;
		} else {
			// Get the next waypoint
			currentWaypoint = backdoorWaypoints[++backdoorWaypointsIndex];
		}
	}
	
	/**
	 * Calculates the next smart waypoint to take and writes it to currentWaypoint.
	 * @throws GameActionException
	 */
	public static void getSmartWaypoint() throws GameActionException {
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
	
	
	/**
	 * Moves to a square that is closer to the goal
	 * returns true if was able to move, false otherwise
	 * @param goalLoc
	 * @return
	 * @throws GameActionException
	 */
	public static boolean moveCloser(MapLocation goalLoc) throws GameActionException {
		// first try to get closer
		double distance = rc.getLocation().distanceSquaredTo(goalLoc);
		moveloop: for (int i=0; i<8; i++) {
			if (rc.canMove(Direction.values()[i])) {
				MapLocation nextLoc = rc.getLocation().add(Direction.values()[i]);
				if (nextLoc.distanceSquaredTo(goalLoc) < distance) {
					rc.move(Direction.values()[i]);
					return true;
				}
			}
		}
		return false;
	}
	/** 5 by 5 bffs
	 * 
	 * @param encArray
	 * @param goalx
	 * @param goaly
	 * @return
	 */
	public static int[] runBFS(int[][] encArray, int goalx, int goaly) {
		int[][] distanceArray = new int[encArray.length][encArray[0].length];
		distanceArray[2][2] = 1;
		for (int y = 0; y<5; y++) {
			for (int x=0; x<5; x++) {
				if (encArray[y][x] > 0) {
					distanceArray[y][x] = -1;
				}
			}
		}
		
		int currValue = 1;
		
		whileLoop: while(currValue < 25) {			
			for (int y = 0; y<5; y++) {
				for (int x=0; x<5; x++) {
					if (distanceArray[y][x] == currValue) {
						if (y == goaly && x == goalx) {
							break whileLoop;
						} else {
							propagate(distanceArray, x, y, currValue + 1);
						}
					}
				}
			}
			currValue++;
		}
		
		int shortestDist = distanceArray[goaly][goalx] - 1;
		int[] output = new int[shortestDist];
		currValue = shortestDist;
		int currx = goalx;
		int curry = goaly;
		int turn = 0;
		
		while(currValue > 1) {
			int[][] neighbors = getNeighbors(currx, curry);
			forloop: for (int[] neighbor: neighbors) {
				int nx = neighbor[0];
				int ny = neighbor[1];
				if (ny < 5 && ny >= 0 && nx < 5 && nx >= 0) {
					if (distanceArray[ny][nx] == currValue) {
						turn = computeTurn(currx, curry, nx, ny);						
						output[currValue-1] = turn;
						currx = nx;
						curry = ny;
						currValue--;
						break forloop;
					}
				}
			}
		}
		output[0] = computeTurn(currx, curry, 2, 2);
		return output;
	}
	
	/**
	 * given an array and a coordinate and a value, propagate value to the neighbors of the coordinate
	 * @param distanceArray
	 * @param y
	 * @param x
	 * @param value
	 */
	public static void propagate(int[][] distanceArray, int x, int y, int value) {
		int[][] neighbors = getNeighbors(x,y);
		for (int[] neighbor: neighbors) {
			int ny = neighbor[1];
			int nx = neighbor[0];
			if (ny < 5 && ny >= 0 && nx < 5 && nx >= 0) {
				if (distanceArray[ny][nx] > value || distanceArray[ny][nx] == 0) {
					distanceArray[ny][nx] = value;
					
				}
			}
		}
	}
	
	public static int[][] getNeighbors(int x, int y) {
		int[][] output = {{x-1, y}, {x-1, y+1}, {x, y+1}, {x+1, y+1},{x+1, y}, {x+1, y-1}, {x, y-1}, {x-1, y-1}};
		return output;
	}
	
	public static int computeTurn(int x, int y, int nx, int ny) {
		if (ny == y+1 && nx == x) {
			return 0;
		} else if (ny == y+1 && nx == x-1) {
			return 1;
		} else if (ny == y && nx == x-1) {
			return 2;
		} else if (ny == y-1 && nx == x-1) {
			return 3;
		} else if (ny == y-1 && nx == x) {
			return 4;
		} else if (ny == y-1 && nx == x+1) {
			return 5;
		} else if (ny == y && nx == x+1) {
			return 6;
		} else if (ny == y+1 && nx == x+1) {
			return 7;
		} else {
			return 8;
		}
	}
	
	public static int[] locToIndex(MapLocation ref, MapLocation test,int offset){
		int[] index = new int[2];
		index[0] = test.y-ref.y+offset;
		index[1] = test.x-ref.x+offset;
		return index;
	}
	
	public static int[][] populate5by5board() throws GameActionException{
		
		MapLocation myLoc=rc.getLocation();
		int[][] array = new int[5][5];

		Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class,8);
//		rc.setIndicatorString(2, "number of bots: "+nearbyRobots.length);
		for (Robot aRobot:nearbyRobots){
			RobotInfo info = rc.senseRobotInfo(aRobot);
			int[] index = locToIndex(myLoc,info.location,2);
			if(index[0]>=0&&index[0]<=4&&index[1]>=0&&index[1]<=4){
				if (info.type != RobotType.SOLDIER) {
					array[index[0]][index[1]]=100;
				}
			}
		}
		return array;
	}

}