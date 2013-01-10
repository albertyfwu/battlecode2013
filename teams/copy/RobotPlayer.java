package copy;

import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

public class RobotPlayer {
	public static void run(RobotController rc) {
		while (true) {
			try {
				if (rc.getType() == RobotType.HQ) {
					if (rc.isActive()) {
						// Spawn a soldier
						Direction desiredDir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
						Direction dir = getSpawnDirection(rc, desiredDir);
						if (dir != null) {
							rc.spawn(dir);
						}
					}
				} else if (rc.getType() == RobotType.SOLDIER) {
					if (rc.isActive()) {
						if (checkFriendlyRobotsNearby(rc, 32, 5) ) {
							if (!checkEnemyRobotsNearby(rc, 4)) {
								Direction desiredDir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
								Direction dir = getMovementDirection(rc, desiredDir);
								if (badBomb(rc, rc.getLocation().add(dir))) {
									rc.defuseMine(rc.getLocation().add(dir));
								} else {
									rc.move(dir);
									rc.setIndicatorString(0, "Last direction moved: "+dir.toString());
								}
							}
//						} else {
//							Direction dir = Direction.values()[(int)(Math.random()*8)];
//							if (rc.canMove(dir)) {
//								rc.move(dir);
//								rc.setIndicatorString(0, "Last direction moved: "+dir.toString());
//							}
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
	
	/**
	 * helper fcn to see if there are any enemy robots within range of given parameter range
	 * @param rc
	 * @param range
	 * @return
	 */
	private static boolean checkEnemyRobotsNearby (RobotController rc, int range) {
		if (rc.senseNearbyGameObjects(Robot.class, range, rc.getTeam().opponent()).length == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * helper fcn to see what direction to actually go given a desired direction
	 * @param rc
	 * @param dir
	 * @return
	 */
	private static Direction getMovementDirection(RobotController rc, Direction dir) {
		if (rc.canMove(dir)) {
			return dir;
		} else if (rc.canMove(dir.rotateLeft())) {
			return dir.rotateLeft();
		} else if (rc.canMove(dir.rotateRight())) {
			return dir.rotateRight();
		} else {
			return null;
		}
	}
	
	/**
	 * helper fcn to see what direction to actually go given a desired direction
	 * @param rc
	 * @param dir
	 * @return
	 */
	private static Direction getSpawnDirection(RobotController rc, Direction dir) {
		if (rc.canMove(dir)) {
			return dir;
		} else if (rc.canMove(dir.rotateLeft())) {
			return dir.rotateLeft();
		} else if (rc.canMove(dir.rotateRight())) {
			return dir.rotateRight();
		} else if (rc.canMove(dir.rotateLeft().rotateLeft())) {
			return dir.rotateLeft().rotateLeft();
		} else if (rc.canMove(dir.rotateRight().rotateRight())) {
			return dir.rotateRight().rotateRight();
		} else if (rc.canMove(dir.rotateLeft().opposite())) {
			return dir.rotateLeft().opposite();
		} else if (rc.canMove(dir.rotateRight().opposite())) {
			return dir.rotateRight().opposite();
		} else {
			return dir.opposite();
		}
	}
	
	/**
	 * helper fcn to compute if location contains badbomb
	 * @param rc
	 * @param loc
	 * @return
	 */
	private static boolean badBomb(RobotController rc, MapLocation loc) {
		Team isBomb = rc.senseMine(loc);
		if (isBomb == null || isBomb == rc.getTeam()) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Helper fcn to see if there are >= n friendly robots within a range of the robot
	 * @param rc
	 * @param range
	 * @param n
	 * @return
	 */
	private static boolean checkFriendlyRobotsNearby (RobotController rc, int range, int n) {
		if (rc.senseNearbyGameObjects(Robot.class, range, rc.getTeam()).length >= n) {
			return true;
		} else {
			return false;
		}
	}
}
