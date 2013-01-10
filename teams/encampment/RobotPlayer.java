package encampment;

import java.util.HashSet;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class RobotPlayer {
	
	/**
	 * Computes MapLocation of nearest non-allied encampment
	 * 
	 * @param rc
	 * @return result
	 */
	public static MapLocation senseClosestNonAlliedEncampment(RobotController rc) {
		MapLocation[] allEncampments = rc.senseAllEncampmentSquares();
		MapLocation[] alliedEncampments = rc.senseAlliedEncampmentSquares();
		HashSet<MapLocation> alliedEncampmentsSet = new HashSet<MapLocation>();
		for (MapLocation location : alliedEncampments) {
			alliedEncampmentsSet.add(location);
		}
		int minDistance = Integer.MAX_VALUE;
		MapLocation result = null;
		for (MapLocation location : allEncampments) {
			if (!alliedEncampmentsSet.contains(location)) {
				int currentDistance = rc.getLocation().distanceSquaredTo(location);
				if (currentDistance < minDistance) {
					minDistance = currentDistance;
					result = location;
				}
			}
		}
		return result;
	}
	
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
					MapLocation encampmentLocation = senseClosestNonAlliedEncampment(rc);
					Direction dir = rc.getLocation().directionTo(encampmentLocation);
					if (dir == Direction.OMNI) { // already there
						rc.captureEncampment(RobotType.ARTILLERY);
					} else if (rc.canMove(dir)) {
						if (Math.random() < 0.5) {
							rc.move(dir);
						} else {
							if(rc.senseMine(rc.getLocation()) == null) {
								rc.layMine();
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
