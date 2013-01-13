package base;

import battlecode.common.RobotController;

public class RobotPlayer {
	
	public static void run(RobotController rc) {
		BaseRobot robot = null;
		
		try {
			switch(rc.getType()) {
			case HQ:
				robot = new HQRobot(rc);
				break;
			case SOLDIER:
				robot = new SoldierRobot(rc);
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
			robot.loop();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
