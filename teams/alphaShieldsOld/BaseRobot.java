package alphaShieldsOld;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public abstract class BaseRobot {
	
	public RobotController rc;
	public int id;
	
	public Strategy strategy;
	
	public boolean enemyNukeHalfDone = false;
	public boolean ourNukeHalfDone = false;
	
	// variables describing the line from our hq to the enemy hq
	public double lineA, lineB, lineC, lineDistanceDenom;
	public int x1, y1, x2, y2; // coordinates of our hq and enemy hq
	
	// Default constructor
	public BaseRobot(RobotController myRC) {
		rc = myRC;
		id = rc.getRobot().getID();
		
		DataCache.init(this); // this must come first
		BroadcastSystem.init(this);
		EncampmentJobSystem.init(this);
		
		initializeLine();
		
		// find out what strategy we're using
//		getStrategy();
	}
	
	public void initializeLine() {
		x1 = DataCache.ourHQLocation.x;
		y1 = DataCache.ourHQLocation.y;
		x2 = DataCache.enemyHQLocation.x;
		y2 = DataCache.enemyHQLocation.y;
		
		if (x2 != x1) {
			lineA = (double)(y2-y1)/(x2-x1);
			lineB = -1;
			lineC = y1 - lineA * x1;
		} else { // x = x_1 \implies 1 * x + 0 * y - x_1 = 0
			lineA = 1;
			lineB = 0;
			lineC = -x1;
		}
		lineDistanceDenom = Math.sqrt(lineA*lineA + lineB*lineB);
	}

	public double distanceToLine(MapLocation location) {
		int x = location.x;
		int y = location.y;
		return Math.abs(lineA * x + lineB * y + lineC) / lineDistanceDenom;
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