package base;

import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;

public class ArtilleryRobot extends BaseRobot {

	public ArtilleryRobot(RobotController rc) {
		super(rc);
	}

	@Override
	public void run() {
		try{
		if (rc.senseEncampmentSquares(rc.getLocation(), 63, rc.getTeam().opponent()).length==0){
		}else{			
		MapLocation placeToShoot= whereToShoot(rc.senseNearbyGameObjects(Robot.class, rc.getLocation(), 63, rc.getTeam().opponent()));
		rc.attackSquare(placeToShoot);
		}
		}catch (Exception e) {
			// Deal with exception
		}
	}
	
	public MapLocation whereToShoot(Robot[] potentialTargets){
		int maxHits=0;
		MapLocation bestLocation=rc.getLocation(potentialTargets[0]);
		for (MapLocation location : potentialTargets){
			int numNeighbors = rc.senseNearbyGameObjects(Robot.class,location,2,rc.getTeam().opponent()).length;
			if (numNeighbors > maxHits){
				maxHits = numNeighbors;
				bestLocation=location;
			}
		}
		return bestLocation;
	}
}
