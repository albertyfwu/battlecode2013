package baseNukeRyan;

import battlecode.common.Clock;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class BaseRobot {
	
	public RobotController rc;
	public int id;
	
	public boolean enemyNukeHalfDone = false;
	public boolean ourNukeHalfDone = false;
	
	// Default constructor
	public BaseRobot(RobotController myRC) {
		rc = myRC;
		id = rc.getRobot().getID();
		DataCache.init(this);
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
			}
			rc.yield();
		}
	}
}