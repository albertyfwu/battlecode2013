package base;

import battlecode.common.RobotController;

public class RobotPlayer {
	
	public static void run(RobotController rc) {
		BaseRobot robot = null;
		
		try {
			switch(rc.getType()) {
			case HQ:
				// TODO: initialize other HQStrategies?
				robot = new HQRobot(rc, HQStrategy.CREATE_SOLDIER);
				break;
			case SOLDIER:
				// Random initialization of soldier strategy
				// TODO: better random seeding?
				double randInt = Math.random(); // randInt \in [0, 1)
				if (randInt < 0.7) {
					robot = new SoldierRobot(rc);
				} else {
					robot = new SoldierRobot(rc);
				}
				break;
			case ARTILLERY:
				robot = new ArtilleryRobot(rc);
				break;
			case GENERATOR:
				robot = new GeneratorRobot(rc);
				break;
			case MEDBAY:
				robot = new MedbayRobot(rc);
				break;
			case SHIELDS:
				robot = new ShieldsRobot(rc);
				break;
			case SUPPLIER:
				robot = new SupplierRobot(rc);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			//
		}
		
		while (true) {
			try {
				robot.loop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
