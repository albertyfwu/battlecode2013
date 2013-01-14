package base;

import battlecode.common.Clock;
import battlecode.common.RobotController;

public class DataCache {
	
	public static BaseRobot robot;
	public static RobotController rc;
	public static int roundNum;
	
	public static void init(BaseRobot myRobot) {
		robot = myRobot;
		rc = robot.rc;
	}
	
	public static void updateRoundVariables() {
	}
}
