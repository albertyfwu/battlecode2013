package writemessage;

import battlecode.common.Clock;
import battlecode.common.RobotController;

public class RobotPlayer {
	public static void run(RobotController rc) {
		while (true) {
			try {
				BroadcastChannel channel = BroadcastSystem.getChannelByType(ChannelType.CHANNEL1);
				if (Clock.getRoundNum() < 10) {
					channel.write(rc, new Message(0, Clock.getRoundNum() + 5));
				}
				System.out.println(channel.read(rc).body);
				// End turn
				if (Clock.getRoundNum() > 100) {
					rc.resign();
				}
				rc.yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
