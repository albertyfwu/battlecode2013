package minelayer;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer {
	public static void run(RobotController rc) {
		while (true) {
			try {
				if (rc.getType() == RobotType.HQ) {
					if (rc.isActive()) {
						// Spawn a soldier
						Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
						if (rc.canMove(dir))
							rc.spawn(dir);
					}
				} else if (rc.getType() == RobotType.SOLDIER) {
					if (rc.senseNearbyGameObjects(Robot.class, 50, rc.getTeam().opponent()).length > 1) { // if there are enemy robots nearby
						for (int i = 0; i < 8; i++) {
							MapLocation location = rc.senseHQLocation().add(Direction.values()[i]);
							if (rc.senseObjectAtLocation(location) == null) {
								Direction dir = rc.getLocation().directionTo(location);
								if (dir != Direction.OMNI && rc.canMove(dir)) {
									rc.move(dir);
									break;
								}
							}
						}
					} else { // lay mines along the way
						if (rc.isActive()) {
							if (rc.senseMine(rc.getLocation()) == null) {
								rc.layMine();
							} else {
								Direction dir = Direction.values()[(rc.getLocation().directionTo(rc.senseEnemyHQLocation()).ordinal() + (int)(Math.random()*4 - 2)) % 8];
								if(rc.canMove(dir)) {
									rc.move(dir);
									rc.setIndicatorString(0, "Last direction moved: "+dir.toString());
								}
							}
						}
					}
				}

				// End turn
				rc.yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
