package alphaShields;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public abstract class EncampmentRobot extends BaseRobot {
	
	public boolean hasCleanedUp = false;
	public int turnCounter = 0;
	
	public boolean designatedForShieldsSuicide = false;

	public EncampmentRobot(RobotController rc) throws GameActionException {
		super(rc);
		sendCompletionMessage();

		if (rc.getLocation().equals(EncampmentJobSystem.shieldsLoc) && rc.getType() != RobotType.SHIELDS) {
			rc.setIndicatorString(0, EncampmentJobSystem.shieldsLoc.toString());
			designatedForShieldsSuicide = true;
		} else {
			designatedForShieldsSuicide = false;
		}
	}
	
	public void sendCompletionMessage() {
		EncampmentJobSystem.postCompletionMessage(rc.getLocation());
	}
	
	@Override
	public void run() {
		MapLocation shieldLoc = EncampmentJobSystem.readShieldLocation();
		if (shieldLoc != null && shieldLoc.equals(rc.getLocation()) && rc.getType() != RobotType.SHIELDS) {
			// TODO: check the channel to see if it's time to suicide
			Message message = BroadcastSystem.read(ChannelType.ARTILLERY_SEEN);
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
