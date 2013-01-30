package alphaRetreat;

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
	}
	
	public void sendCompletionMessage() {
		EncampmentJobSystem.postCompletionMessage(rc.getLocation());
	}
	
	@Override
	public void run() {
		MapLocation shieldLoc = EncampmentJobSystem.readShieldLocation();
		if (shieldLoc != null && shieldLoc.equals(rc.getLocation()) && rc.getType() != RobotType.SHIELDS) {
			// check the channel to see if it's time to suicide
			rc.suicide();
		}
		runMain();
	}
	
	abstract public void runMain();
}
