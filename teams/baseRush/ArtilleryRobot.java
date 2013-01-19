package baseRush;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;

public class ArtilleryRobot extends BaseRobot {
		
	public ArtilleryRobot(RobotController rc) {
		super(rc);
	}

	@Override
	public void run() {
		try {
			if (rc.isActive()) {
				Robot[] potentialTargets = rc.senseNearbyGameObjects(Robot.class, rc.getLocation(), 63, rc.getTeam().opponent());
				MapLocation target = getBestTarget(potentialTargets);
				if (target != null && rc.canAttackSquare(target)) {
					rc.attackSquare(target);
				}
			}
		} catch (Exception e) {
			//
		}
	}
	
	// TODO: Make sure GameActionException doesn't get thrown
	public MapLocation getBestTarget(Robot[] potentialTargets) throws GameActionException {
		int maxHits = 0;
		MapLocation bestLocation = null;
		for (Robot potentialTarget : potentialTargets){
			MapLocation location = rc.senseLocationOf(potentialTarget);
			int numNeighbors = rc.senseNearbyGameObjects(Robot.class, location, 2, rc.getTeam().opponent()).length;
			if (numNeighbors > maxHits){
				maxHits = numNeighbors;
				bestLocation = location;
			}
		}
		return bestLocation;
	}
}
