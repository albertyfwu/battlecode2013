package baseTurgid;

import battlecode.common.RobotController;


public abstract class EncampmentRobot extends BaseRobot{
	
	public boolean hasCleanedUp = false;
	public int turnCounter = 0;
//	public ChannelType cleanUpChannel;
//	public int cleanUpWait = Constants.CLEAN_UP_WAIT_TIME;

	public EncampmentRobot(RobotController rc) {
		super(rc);
		sendCompletionMessage();
		
		// TODO Auto-generated constructor stub
	}
	
	public void sendCompletionMessage() {
		EncampmentJobSystem.postCompletionMessage(rc.getLocation());
	}
	
//	public void cleanUp() {
//		EncampmentJobSystem.postCleanUp(cleanUpChannel);
//		hasCleanedUp = true;
//	}
	
	

}
