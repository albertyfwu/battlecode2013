package base;

import battlecode.common.RobotController;

public abstract class BaseRobot {
	
	public RobotController rc;
	
	// Default constructor
	public BaseRobot(RobotController myRC) {
		rc = myRC;
	}
	
	// Actions for a specific robot
	abstract public void run();
	
	public void loop() {
		// Initialize BroadcastSystem
		NavSystem.init(this);
		BroadcastSystem.init(this);
		EncampmentJobSystem.init(this);
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