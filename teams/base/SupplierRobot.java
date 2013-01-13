package base;

import battlecode.common.RobotController;

public class SupplierRobot extends EncampmentRobot {

	public SupplierRobot(RobotController rc) {
		super(rc);
		sendCompletionMessage();
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
		// TODO Auto-generated method stub

	}

}
