package baseShields;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class ShieldsRobot extends BaseRobot {

	public MapLocation location;
	
	public ShieldsRobot(RobotController rc) {
		super(rc);
		location = rc.getLocation();
	}

	@Override
	public void run() {
		// Broadcast location
		BroadcastSystem.write(ChannelType.SHIELDS, location.x << 8 + location.y);
	}

}
