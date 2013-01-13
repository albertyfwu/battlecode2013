package base;

import java.util.Random;

import battlecode.common.Clock;
import battlecode.common.RobotController;

public abstract class BaseRobot {
	
	public RobotController rc;
	public int id;
	public Random aRandom;
	
	// Default constructor
	public BaseRobot(RobotController myRC) {
		rc = myRC;
		id = rc.getRobot().getID();
		NavSystem.init(this);
		BroadcastSystem.init(this);
		EncampmentJobSystem.init(this);
		// Create a unique seed
		aRandom = new Random();
		aRandom.setSeed(Clock.getBytecodeNum() * id);
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