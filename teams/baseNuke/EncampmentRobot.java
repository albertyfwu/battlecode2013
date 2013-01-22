package baseNuke;

import battlecode.common.RobotController;

public abstract class EncampmentRobot extends BaseRobot {
	
	public boolean hasCleanedUp = false;
	public int turnCounter = 0;

	public EncampmentRobot(RobotController rc) {
		super(rc);
		sendCompletionMessage();
	}
	
	public void sendCompletionMessage() {
		EncampmentJobSystem.postCompletionMessage(rc.getLocation());
	}
}
