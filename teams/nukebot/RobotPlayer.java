package nukebot;

import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Upgrade;

public class RobotPlayer {
	public static void run(RobotController rc) {
		while (true) {
			try {
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
