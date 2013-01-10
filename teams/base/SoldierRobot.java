package base;

import battlecode.common.RobotController;

public class SoldierRobot extends BaseRobot {

	public Platoon platoon = null;
	
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
			BroadcastChannel channel = BroadcastSystem.getChannelByType(ChannelType.values()[0]);
			Message message = channel.read(rc);
			if (message != null) {
				byte header = (byte)message.header;
				short body = (short)message.body;
				rc.setIndicatorString(0, Integer.toString(header));
				rc.setIndicatorString(1, Integer.toString(body));
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
