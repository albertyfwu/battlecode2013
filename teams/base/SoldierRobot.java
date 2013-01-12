package base;

import java.util.ArrayList;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

public class SoldierRobot extends BaseRobot {

	public Platoon platoon = null;
	public boolean calculatedPath = false;
	
	// TODO: Testing
	public ArrayList<MapLocation> wayPoints;
	public int wayPointsSize;
	public int wayPointsIndex;
	
	public SoldierRobot(RobotController rc) {
		super(rc);
	}
	
	public SoldierRobot(RobotController rc, Platoon platoon) {
		super(rc);
		this.platoon = platoon;
	}

	@Override
	public void run() {
		try {
//			BroadcastChannel channel = BroadcastSystem.getChannelByType(ChannelType.values()[0]);
//			Message message = channel.read(rc);
//			if (message != null) {
//				byte header = (byte)message.header;
//				short body = (short)message.body;
//				rc.setIndicatorString(0, Integer.toString(header));
//				rc.setIndicatorString(1, Integer.toString(body));
//			}
			
			// Try to go to a coordinate
			// Try to go to (39, 27) on choice.xml map
			MapLocation end = new MapLocation(39, 27);
			MapLocation start = rc.getLocation();
			if (!calculatedPath) {
				calculatedPath = true;
				wayPoints = PathFinder.calculatePath(rc, start, end);
				wayPointsSize = wayPoints.size();
			}

			// TODO: write a function for following waypoints
			if (wayPoints != null) {
				// we have waypoints, so follow them
				if (rc.getLocation().distanceSquaredTo(wayPoints.get(wayPointsIndex)) <= 5) {
					if (wayPointsIndex < wayPointsSize - 1) {
						wayPointsIndex++;
					}
				}
				goToLocation(wayPoints.get(wayPointsIndex));
			}
			
			
//			// TODO: do some broadcast reading, listen to leader of platoon, etc...
//			switch (this.platoon.getStrategy()) {
//			case KITE:
//				// 
//				break;
//			default:
//				break;
//			}
//			// TODO: check if we should change the strategy?
		} catch (Exception e) {
			// Deal with exception
		}
	}
	
	public Platoon getPlatoon() {
		return this.platoon;
	}

	
	
	
	
	
	
	
	
	
	private static void goToLocation(MapLocation whereToGo) throws GameActionException {
		int dist = rc.getLocation().distanceSquaredTo(whereToGo);
		if (dist>0&&rc.isActive()){
			Direction dir = rc.getLocation().directionTo(whereToGo);
			goDirectionAndDefuse(dir);
		}
	}
	
	private static void goToLocationAvoidMines(MapLocation whereToGo) throws GameActionException {
		int dist = rc.getLocation().distanceSquaredTo(whereToGo);
		if (dist>0&&rc.isActive()){
			Direction dir = rc.getLocation().directionTo(whereToGo);
			goDirectionAvoidMines(dir);
		}
	}
	
	private static void goDirectionAndDefuse(Direction dir) throws GameActionException {
		int[] directionOffsets = {0,1,-1,2,-2};
		Direction lookingAtCurrently = dir;
		lookAround: for (int d:directionOffsets){
			lookingAtCurrently = Direction.values()[(dir.ordinal()+d+8)%8];
			if(rc.canMove(lookingAtCurrently)){
				if (badBomb(rc.getLocation().add(lookingAtCurrently))) {
					rc.defuseMine(rc.getLocation().add(lookingAtCurrently));
				} else {
					rc.move(lookingAtCurrently);
					rc.setIndicatorString(0, "Last direction moved: "+lookingAtCurrently.toString());
				}
				break lookAround;
			}
		}
	}
	
	private static void goDirectionAvoidMines(Direction dir) throws GameActionException {
		int[] directionOffsets = {0,1,-1,2,-2};
		Direction lookingAtCurrently = dir;
		lookAround: for (int d:directionOffsets){
			lookingAtCurrently = Direction.values()[(dir.ordinal()+d+8)%8];
			if(rc.canMove(lookingAtCurrently)){
				if (!badBomb(rc.getLocation().add(lookingAtCurrently))) {
					rc.move(lookingAtCurrently);
					rc.setIndicatorString(0, "Last direction moved: "+lookingAtCurrently.toString());
				}
				break lookAround;
			}
		}
	}

	private static MapLocation findRallyPoint() {
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
		MapLocation ourLoc = rc.senseHQLocation();
		int x = (enemyLoc.x+3*ourLoc.x)/4;
		int y = (enemyLoc.y+3*ourLoc.y)/4;
		MapLocation rallyPoint = new MapLocation(x,y);
		return rallyPoint;
	}

	public static void hqCode() throws GameActionException{
//		Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class,100000,rc.getTeam());
//		if (alliedRobots.length > 20 && Clock.getRoundNum() > 0) {
//			if (!rc.hasUpgrade(Upgrade.DEFUSION)) {
//				rc.researchUpgrade(Upgrade.DEFUSION);
//			}
//		}

		if (rc.isActive()) {
			// Spawn a soldier
				Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
				if (rc.canMove(dir))
					rc.spawn(dir);
			
		}
	}
	
	/**
	 * helper fcn to compute if location contains a bad bomb
	 * @param rc
	 * @param loc
	 * @return
	 */
	private static boolean badBomb(MapLocation loc) {
		Team isBomb = rc.senseMine(loc);
		if (isBomb == null || isBomb == rc.getTeam()) {
			return false;
		} else {
			return true;
		}
	}
}
