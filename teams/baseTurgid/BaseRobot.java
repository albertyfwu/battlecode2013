package baseTurgid;

import battlecode.common.RobotController;

public abstract class BaseRobot {
	
	public RobotController rc;
	public int id;
	
	public boolean enemyNukeHalfDone = false;
	
	// Default constructor
	public BaseRobot(RobotController myRC) {
		rc = myRC;
		id = rc.getRobot().getID();
		DataCache.init(this); // this must come first
		BroadcastSystem.init(this);
		EncampmentJobSystem.init(this);
	}
	
	// Actions for a specific robot
	abstract public void run();
	
	public void loop() {
		while (true) {
			try {
				run();
			} catch (Exception e) {
				// Deal with exception
				e.printStackTrace();
			}
			rc.yield();
		}
	}
}