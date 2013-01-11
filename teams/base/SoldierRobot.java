package base;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class SoldierRobot extends BaseRobot {

	public Platoon platoon = null;
	public boolean calculatedPath = false;
	
	public SoldierRobot(RobotController rc) {
		super(rc);
	}
	
	public SoldierRobot(RobotController rc, Platoon platoon) {
		super(rc);
		this.platoon = platoon;
	}

	@Override
	public void run() {
		try {
//			BroadcastChannel channel = BroadcastSystem.getChannelByType(ChannelType.values()[0]);
//			Message message = channel.read(rc);
//			if (message != null) {
//				byte header = (byte)message.header;
//				short body = (short)message.body;
//				rc.setIndicatorString(0, Integer.toString(header));
//				rc.setIndicatorString(1, Integer.toString(body));
//			}
			
			// Try to go to a coordinate
			// Try to go to (39, 25) on choice.xml map
			MapLocation end = new MapLocation(39, 27);
			MapLocation start = rc.getLocation();
			if (!calculatedPath) {
				calculatedPath = true;
				HPA.calculatePath(rc, start, end);
			}
			
			
//			// TODO: do some broadcast reading, listen to leader of platoon, etc...
//			switch (this.platoon.getStrategy()) {
//			case KITE:
//				// 
//				break;
//			default:
//				break;
//			}
//			// TODO: check if we should change the strategy?
		} catch (Exception e) {
			// Deal with exception
		}
	}
	
	public Platoon getPlatoon() {
		return this.platoon;
	}

}
