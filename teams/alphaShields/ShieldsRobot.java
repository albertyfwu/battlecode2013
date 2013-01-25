package alphaShields;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class ShieldsRobot extends BaseRobot {

	MapLocation location;
	int x, y; // coordinates of the shields robot
	int message;
	
	public ShieldsRobot(RobotController rc) {
		super(rc);
		location = rc.getLocation();
		x = location.x;
		y = location.y;
		message = (x << 8) + y;
	}

	@Override
	public void run() {
		BroadcastSystem.write(ChannelType.SHIELDS, message);
	}

}
