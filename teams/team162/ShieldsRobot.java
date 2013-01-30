package team162;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class ShieldsRobot extends EncampmentRobot {

	public ShieldsRobot(RobotController rc) throws GameActionException {
		super(rc);
	}

	@Override
	public void runMain() {

	}
	
	@Override
	public void sendCompletionMessage() {
		EncampmentJobSystem.postShieldCompletionMessage(rc.getLocation());
	}

}
