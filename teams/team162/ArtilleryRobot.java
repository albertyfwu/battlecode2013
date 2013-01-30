package team162;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;

public class ArtilleryRobot extends EncampmentRobot {
	
	public ArtilleryRobot(RobotController rc) throws GameActionException {
		super(rc);
	}

	@Override
	public void runMain() {
		try {
			if (rc.isActive()) {
				Robot[] potentialTargets = rc.senseNearbyGameObjects(Robot.class, rc.getLocation(), 63, rc.getTeam().opponent());
				MapLocation target = getBestTarget(potentialTargets);
				if (target != null && rc.canAttackSquare(target)) {
					rc.attackSquare(target);
				}
			}
		} catch (Exception e) {
//			e.printStackTrace();
		}
	}

	public MapLocation getBestTarget(Robot[] potentialTargets) throws GameActionException {
		int highestScore = 39;
		MapLocation bestLocation = null;
		
		for (Robot potentialTarget : potentialTargets) {
			int currentScore = 40;
			MapLocation location = rc.senseLocationOf(potentialTarget);
			Robot[] splashRobots = rc.senseNearbyGameObjects(Robot.class, location, GameConstants.ARTILLERY_SPLASH_RADIUS_SQUARED, null);
			for (Robot splashRobot : splashRobots) {
				if (splashRobot.getTeam() == rc.getTeam()) {
					currentScore -= 15;
				} else {
					currentScore += 15;
				}
			}
			if (currentScore > highestScore) {
				bestLocation = location;
				highestScore = currentScore;
			}
		}
		return bestLocation; // will return null if there were no positive score enemy targets
	}
}
