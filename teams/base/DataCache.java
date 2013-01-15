package base;

import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;

public class DataCache {
	
	public static BaseRobot robot;
	public static RobotController rc;
	
	// Round variables
	public static int numAlliedRobots;
	public static int numAlliedEncampments;
	public static int numAlliedSoldiers;
	public static int numNearbyEnemyRobots;
	public static int numTotalEnemyRobots;
	
	public static void init(BaseRobot myRobot) {
		robot = myRobot;
		rc = robot.rc;
	}
	
	/**
	 * A function that updates round variables
	 */
	public static void updateRoundVariables() throws GameActionException {
		numAlliedRobots = rc.senseNearbyGameObjects(Robot.class, 10000, rc.getTeam()).length;
		numAlliedEncampments = rc.senseEncampmentSquares(rc.getLocation(), 10000, rc.getTeam()).length;
		numAlliedSoldiers = numAlliedRobots - numAlliedEncampments - 1 - EncampmentJobSystem.maxEncampmentJobs;
		numNearbyEnemyRobots = rc.senseNearbyGameObjects(Robot.class, Constants.RALLYING_RADIUS_SQUARED_CHECK, rc.getTeam().opponent()).length;
		numTotalEnemyRobots = rc.senseNearbyGameObjects(Robot.class, 10000, rc.getTeam().opponent()).length;		
	}
}
