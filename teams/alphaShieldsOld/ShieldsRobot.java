package alphaShieldsOld;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class ShieldsRobot extends BaseRobot {

	MapLocation location;
	int x, y; // coordinates of the shields robot
	int coordinates;
	
	public ShieldsRobot(RobotController rc) {
		super(rc);
		location = rc.getLocation();
		x = location.x;
		y = location.y;
		coordinates = (x << 8) + y;
	}

	@Override
	public void run() {
		try {
			// Find out how many empty spaces there are next to the shield
			int emptySpaces = 0;
			for (int i = 8; --i >= 0; ) {
				MapLocation iterLocation = location.add(DataCache.directionArray[i]);
				if (rc.senseObjectAtLocation(iterLocation) == null) {
					emptySpaces++;
				}
			}
			
			BroadcastSystem.write(ChannelType.SHIELDS, (emptySpaces << 16) + coordinates);
			
			// Ping back to the broadcast channel
			BroadcastSystem.write(ChannelType.SHIELDS_PING, Clock.getRoundNum());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
