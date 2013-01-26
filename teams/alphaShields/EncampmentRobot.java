package alphaShields;

import battlecode.common.RobotController;

public abstract class EncampmentRobot extends BaseRobot {
	
	public boolean hasCleanedUp = false;
	public int turnCounter = 0;
	
	public boolean designatedForShieldsSuicide = false;

	public EncampmentRobot(RobotController rc) {
		super(rc);
		sendCompletionMessage();
		// need to find out if this encampment is the one that is designated for shields encampment
		// TODO:
		designatedForShieldsSuicide = false;
	}
	
	public void sendCompletionMessage() {
		EncampmentJobSystem.postCompletionMessage(rc.getLocation());
	}
	
	@Override
	public void run() {
		if (designatedForShieldsSuicide) {
			// TODO: check the channel to see if it's time to suicide
			Message message = BroadcastSystem.read(ChannelType.ENCAMPMENT_SUICIDE);
			if (message.isValid){
				int body = message.body;
				if (body == Constants.TRUE) {
					// Suicide!
					rc.suicide();
				}
			}
		}
		runMain();
	}
	
	abstract public void runMain();
}
