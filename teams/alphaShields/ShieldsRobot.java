package alphaShields;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class ShieldsRobot extends EncampmentRobot {

	public ShieldsRobot(RobotController rc) throws GameActionException {
		super(rc);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void runMain() {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void sendCompletionMessage() {
		EncampmentJobSystem.postShieldCompletionMessage(rc.getLocation());
	}

}
