package base;

import battlecode.common.RobotController;

public class GeneratorRobot extends EncampmentRobot {

	public GeneratorRobot(RobotController rc) {
		super(rc);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		try {
			if (!hasCleanedUp) {
				turnCounter++;
				if (turnCounter >= cleanUpWait) {
					cleanUp();
				}
			}
		} catch(Exception e) {
			
		}
	}

}
