package base;

import battlecode.common.RobotController;

public abstract class BaseRobot {
	
	public RobotController rc;
	
	// Default constructor
	public BaseRobot(RobotController rc) {
		this.rc = rc;
	}
	
	// Actions for a specific robot
	abstract public void run();
	
	public void loop() {
		while (true) {
			try {
				run();
			} catch (Exception e) {
				// Deal with exception
			}
			rc.yield();
		}
	}
}