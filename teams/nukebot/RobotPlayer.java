package nukebot;

import java.util.Random;

import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Upgrade;

public class RobotPlayer {
	public static void run(RobotController rc) {
		while (true) {
			try {
				Random randomGenerator = new Random();
				for (int i = 1; i< 30; i++) {
					int channel = randomGenerator.nextInt(10000);
					int msg = randomGenerator.nextInt(10000);
					rc.broadcast(channel, msg);
				}

				if (rc.getType()==RobotType.HQ){
					if (!rc.hasUpgrade(Upgrade.NUKE)) {
						rc.researchUpgrade(Upgrade.NUKE);
					}
				}
				// End turn
				rc.yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
