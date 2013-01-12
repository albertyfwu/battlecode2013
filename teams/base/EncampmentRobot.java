package base;

import battlecode.common.RobotController;


public abstract class EncampmentRobot extends BaseRobot{
	
	public boolean hasSentCompletion = false;

	public EncampmentRobot(RobotController rc) {
		super(rc);
		// TODO Auto-generated constructor stub
	}
	
	public void sendCompletionMessage() {
		EncampmentJobSystem.postCompletionMessage(rc.getLocation());
	}
	
	public void cleanUp(ChannelType channel) {
		EncampmentJobSystem.postCleanUp(channel);
	}
	
	

}
