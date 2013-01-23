package alphaMemory;

import battlecode.common.RobotController;

public abstract class BaseRobot {
	
	public RobotController rc;
	public int id;
	
	public Strategy strategy;
	
	public boolean enemyNukeHalfDone = false;
	public boolean ourNukeHalfDone = false;
	
	// Default constructor
	public BaseRobot(RobotController myRC) {
		rc = myRC;
		id = rc.getRobot().getID();
		
		DataCache.init(this); // this must come first
		BroadcastSystem.init(this);
		EncampmentJobSystem.init(this);
		
		// find out what strategy we're using
//		getStrategy();
	}
	
//	public void getStrategy() {
//		Message message = BroadcastSystem.read(ChannelType.STRATEGY);
//		if (message.isValid) {
//			strategy = Strategy.values()[message.body];
//		} else {
//			// TODO: choose a strategy
//			strategy = Strategy.UNKNOWN;
//		}
//	}
	
	// Actions for a specific robot
	abstract public void run();
	
	public void loop() {
		while (true) {
			try {
//				if (strategy == Strategy.UNKNOWN) {
//					getStrategy();
//				}
				run();
			} catch (Exception e) {
				// Deal with exception
				e.printStackTrace();
			}
			rc.yield();
		}
	}
}