package basetest;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.Upgrade;

public class DataCache {
	
	public static BaseRobot robot;
	public static RobotController rc;
	
	public static MapLocation ourHQLocation;
	public static MapLocation enemyHQLocation;
	
	// Round variables - army sizes
	public static int numAlliedRobots;
	public static int numAlliedEncampments;
	public static int numAlliedSoldiers;
	public static int numNearbyAlliedRobots;
	public static int numNearbyAlliedEncampments;
	public static int numNearbyAlliedSoldiers;
	public static int numNearbyEnemyRobots;
	public static int numTotalEnemyRobots;
	
	// Round variables - upgrades
	public static boolean have_defusion;
	public static boolean have_pickaxe;
	public static boolean have_vision;
	
	public static void init(BaseRobot myRobot) {
		robot = myRobot;
		rc = robot.rc;
		
		ourHQLocation = rc.senseHQLocation();
		enemyHQLocation = rc.senseEnemyHQLocation();
	}
	
	/**
	 * A function that updates round variables
	 */
	public static void updateRoundVariables() throws GameActionException {
		numAlliedRobots = rc.senseNearbyGameObjects(Robot.class, 10000, rc.getTeam()).length;
		numNearbyAlliedRobots = rc.senseNearbyGameObjects(Robot.class, 14, rc.getTeam()).length;
		numNearbyAlliedEncampments = rc.senseEncampmentSquares(rc.getLocation(), 14, rc.getTeam()).length;
		numNearbyAlliedSoldiers = numNearbyAlliedRobots - numNearbyAlliedEncampments;
		numAlliedEncampments = rc.senseEncampmentSquares(rc.getLocation(), 10000, rc.getTeam()).length;
		numAlliedSoldiers = numAlliedRobots - numAlliedEncampments - 1 - EncampmentJobSystem.maxEncampmentJobs;
		numNearbyEnemyRobots = rc.senseNearbyGameObjects(Robot.class, Constants.RALLYING_SOLDIER_THRESHOLD, rc.getTeam().opponent()).length;
		numTotalEnemyRobots = rc.senseNearbyGameObjects(Robot.class, 10000, rc.getTeam().opponent()).length;
		
		if (!have_defusion) {
			have_defusion = rc.hasUpgrade(Upgrade.DEFUSION);
		}
		if (!have_pickaxe) {
			have_pickaxe = rc.hasUpgrade(Upgrade.PICKAXE);
		}
		if (!have_vision) {
			have_vision = rc.hasUpgrade(Upgrade.VISION);
		}
	}
}
