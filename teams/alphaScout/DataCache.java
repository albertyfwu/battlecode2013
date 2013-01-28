package alphaScout;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Upgrade;

public class DataCache {
	
	public static BaseRobot robot;
	public static RobotController rc;
	
	public static MapLocation ourHQLocation;
	public static MapLocation enemyHQLocation;
	public static int rushDistSquared;
	public static int rushDist;
	
	public static Direction[] directionArray = Direction.values();
	
	// Map width
	public static int mapWidth;
	public static int mapHeight;
	
	// Round variables - army sizes
	// Allied robots
	public static int numAlliedRobots;
	public static int numAlliedSoldiers;
	public static int numAlliedEncampments;
	
	public static int numNearbyAlliedRobots;	
	public static int numNearbyAlliedSoldiers;
	public static int numNearbyAlliedEncampments;

	// Enemy robots
	public static int numEnemyRobots;
	
	public static Robot[] nearbyEnemyRobots;
	
	public static int numNearbyEnemyRobots;
	public static int numNearbyEnemySoldiers;
	
	// Round variables - upgrades
	public static boolean hasFusion;
	public static boolean hasDefusion;
	public static boolean hasPickaxe;
	
	public static void init(BaseRobot myRobot) {
		robot = myRobot;
		rc = robot.rc;
		
		ourHQLocation = rc.senseHQLocation();
		enemyHQLocation = rc.senseEnemyHQLocation();
		rushDistSquared = ourHQLocation.distanceSquaredTo(enemyHQLocation);
		rushDist = (int) Math.sqrt(rushDistSquared);
		
		mapWidth = rc.getMapWidth();
		mapHeight = rc.getMapHeight();
	}
	
	/**
	 * A function that updates round variables
	 */
	public static void updateRoundVariables() throws GameActionException {
		numAlliedRobots = rc.senseNearbyGameObjects(Robot.class, 10000, rc.getTeam()).length;
		numAlliedEncampments = rc.senseEncampmentSquares(rc.getLocation(), 10000, rc.getTeam()).length;
		numAlliedSoldiers = numAlliedRobots - numAlliedEncampments - 1 - EncampmentJobSystem.maxEncampmentJobs;
		
		numNearbyAlliedRobots = rc.senseNearbyGameObjects(Robot.class, 14, rc.getTeam()).length;
		numNearbyAlliedEncampments = rc.senseEncampmentSquares(rc.getLocation(), 14, rc.getTeam()).length;
		numNearbyAlliedSoldiers = numNearbyAlliedRobots - numNearbyAlliedEncampments;
		
		nearbyEnemyRobots = rc.senseNearbyGameObjects(Robot.class, 25, rc.getTeam().opponent());
		
		numNearbyEnemySoldiers = 0;
		for (int i = nearbyEnemyRobots.length; --i >= 0; ) {
			RobotInfo robotInfo = rc.senseRobotInfo(nearbyEnemyRobots[i]);
			if (robotInfo.type == RobotType.SOLDIER) {
				numNearbyEnemySoldiers++;
			}
		}
		
		numNearbyEnemyRobots = nearbyEnemyRobots.length;
		numEnemyRobots = rc.senseNearbyGameObjects(Robot.class, 10000, rc.getTeam().opponent()).length;
		
		if (!hasFusion) {
			hasFusion = rc.hasUpgrade(Upgrade.FUSION);
		}
		if (!hasDefusion) {
			hasDefusion = rc.hasUpgrade(Upgrade.DEFUSION);
		}
		if (!hasPickaxe) {
			hasPickaxe = rc.hasUpgrade(Upgrade.PICKAXE);
		}
	}
}
