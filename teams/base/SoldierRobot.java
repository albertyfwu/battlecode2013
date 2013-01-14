package base;

import java.util.ArrayList;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.RobotInfo;
import battlecode.common.Team;

public class SoldierRobot extends BaseRobot {
	
	public Platoon platoon;
	
	public SoldierState soldierState;
	
	// For mining
	private MapLocation miningCenter;
	private int miningRadius;
	private int miningRadiusSquared;
	private int miningMaxRadius;
	
	public boolean unassigned = true;
	public ChannelType assignedChannel;
	public MapLocation goalLoc;
	
	public MapLocation currentLocation;
	
	public MapLocation HQLocation;
	public MapLocation EnemyHQLocation;

	
	
	

	
	
	public SoldierRobot(RobotController rc) throws GameActionException {
		super(rc);
		
		HQLocation = rc.senseHQLocation();
		EnemyHQLocation = rc.senseEnemyHQLocation();

		ChannelType channel = EncampmentJobSystem.findJob();
		if (channel != null) {
			assignedChannel = channel;
			unassigned = false;
			goalLoc = EncampmentJobSystem.goalLoc;
			System.out.println("channel: " + channel);
			System.out.println("goalLocx: " + goalLoc.x);
			System.out.println("goalLocy: " + goalLoc.y);
			EncampmentJobSystem.updateJobTaken(channel);
		}
	}
	
//	public SoldierRobot(RobotController rc, Platoon platoon) {
//		super(rc);
//		this.platoon = platoon;
//	}

	@Override
	public void run() {
		try {
			currentLocation = rc.getLocation();
			if (Clock.getRoundNum() < 2500) {
				if (Clock.getRoundNum() > 250) {
					if (soldierState != SoldierState.FIGHTING){
						soldierState = SoldierState.FIGHTING;
					}
					if (soldierState == SoldierState.FIGHTING) {
						microCode();
					}
				} else {
					NavSystem.goToLocation(findRallyPoint());
				}
				

				//				if (soldierState != SoldierState.MINING_IN_CIRCLE) {
				//					setupCircleMining(new MapLocation(15, 27), 10);
				//				}
				//				if (rc.isActive()) {
				//					mineInCircle();
				//				}

			} else {
				//			} else {
				////				if (NavSystem.navMode == NavMode.NEUTRAL) {
				////					NavSystem.setupSmartNav(rc.senseEnemyHQLocation());
				////				}
				////				NavSystem.followWaypoints();
				//				rc.suicide();
				//				
				//				NavSystem.goToLocation(new MapLocation(10, 10));
				//				if (rc.getLocation().x == 10 && rc.getLocation().y == 10) {
				//					rc.suicide();
				//				}

				currentLocation = rc.getLocation();

				if (unassigned && rc.isActive()) {
					if (NavSystem.navMode == NavMode.NEUTRAL) {
						NavSystem.setupSmartNav(new MapLocation(10, 10));
					}
					NavSystem.followWaypoints();

					if (rc.getLocation().x == 10 && rc.getLocation().y == 10) {
						rc.suicide();
					}
				} else { // is assigned to an encampment job
					captureCode();
				}
			}
						
			
		} catch (Exception e) {
			System.out.println("caught exception before it killed us:");
			System.out.println(rc.getRobot().getID());
			e.printStackTrace();
		}
	}
	
	public Platoon getPlatoon() {
		return this.platoon;
	}
	
	/**
	 * Set up a center MapLocation for mining in a circle
	 * @param center
	 */
	private void setupCircleMining(MapLocation center, int maxRadius) {
		soldierState = SoldierState.MINING_IN_CIRCLE;
		miningCenter = center;
		miningMaxRadius = maxRadius;
		miningRadius = Constants.INITIAL_MINING_RADIUS;
		miningRadiusSquared = miningRadius * miningRadius;
	}
	
	/**
	 * This method tells the soldier to mine in a circle (as set up by setupCircleMining())
	 * @return true if we can still mine, and false if the circle radius has exceeded the maxMiningRadius
	 * @throws GameActionException
	 */
	private boolean mineInCircle() throws GameActionException {
		if (rc.isActive()) {
			if (minesDenselyPacked(miningCenter, miningRadiusSquared)) {
				// mines are fairly dense, so expand the circle in which to mine
				miningRadius += Constants.MINING_RADIUS_DELTA;
				if (miningRadius > miningMaxRadius) {
					return false;
				}
				miningRadiusSquared = miningRadius * miningRadius;
			}
			if (rc.getLocation().distanceSquaredTo(miningCenter) >= miningRadiusSquared) {
				// If we're too far from the center, move closer
				NavSystem.goToLocation(miningCenter);
			} else if (rc.getLocation().distanceSquaredTo(miningCenter) <= Math.pow(miningRadius - Constants.MINING_CIRCLE_DR_TOLERANCE, 2)) {
				// If we're too close to the center, move away
				NavSystem.goDirectionAndDefuse(rc.getLocation().directionTo(miningCenter).opposite());
			} else {
				// Lay a mine if possible
				if (rc.senseMine(rc.getLocation()) == null) {
					rc.layMine();
				}
				// Walk around the circle
				Direction dir = rc.getLocation().directionTo(miningCenter).rotateLeft().rotateLeft(); // move counterclockwise around circle
				NavSystem.goDirectionAndDefuse(dir);
			}
		}
		return true;
	}
	
	private int getNumAlliedNeighbors() {
		return rc.senseNearbyGameObjects(Robot.class, 2, rc.getTeam()).length;
	}
	
	private int getNumAlliedNeighborsSquare(MapLocation square) {
		return rc.senseNearbyGameObjects(Robot.class, square, 2, rc.getTeam()).length;

	}
	
	private double[] getEnemies2Or3StepsAway() throws GameActionException {
		double count2 = 0;
		double count3 = 0;
		Robot[] enemiesInVision = rc.senseNearbyGameObjects(Robot.class, 18, rc.getTeam().opponent());
		for (Robot enemy: enemiesInVision) {
			RobotInfo rinfo = rc.senseRobotInfo(enemy);
			int dist = rinfo.location.distanceSquaredTo(currentLocation);
			if (rinfo.type == RobotType.SOLDIER) {
				if (dist <=8) {
					count2++;
				} else if (dist > 8 && (dist <= 14 || dist == 18)) {
					count3++;
				}
			} else {
				if (dist <=8) {
					count2 += 0.5;
				} else if (dist > 8 && (dist <= 14 || dist == 18)) {
					count3 += 0.5;
				}
			}
			
		}
		
		double[] output = {count2, count3};
		return output;
	}
	
	private double[] getEnemies2Or3StepsAwaySquare(MapLocation square, Team squareTeam) throws GameActionException {
		double count2 = 0;
		double count3 = 0;
		Robot[] enemiesInVision = rc.senseNearbyGameObjects(Robot.class, 18, squareTeam.opponent());
		for (Robot enemy: enemiesInVision) {
			RobotInfo rinfo = rc.senseRobotInfo(enemy);
			int dist = rinfo.location.distanceSquaredTo(currentLocation);
			if (rinfo.type == RobotType.SOLDIER) {
				if (dist <=8) {
					count2++;
				} else if (dist > 8 && (dist <= 14 || dist == 18)) {
					count3++;
				}
			} else {
				if (dist <=8) {
					count2 += 0.5;
				} else if (dist > 8 && (dist <= 14 || dist == 18)) {
					count3 += 0.5;
				}
			}
		}
		
		double[] output = {count2, count3};
		return output;
	}
	
	public int[] getClosestEnemy(Robot[] enemyRobots) throws GameActionException {
		int closestDist = currentLocation.distanceSquaredTo(EnemyHQLocation);
		MapLocation closestEnemy=EnemyHQLocation; // default to HQ

		int dist = 0;
		for (int i=0;i<enemyRobots.length;i++){
			RobotInfo arobotInfo = rc.senseRobotInfo(enemyRobots[i]);
			dist = arobotInfo.location.distanceSquaredTo(rc.getLocation());
			if (dist<closestDist){
				closestDist = dist;
				closestEnemy = arobotInfo.location;
			}
		}
		int[] output = new int[4];
		output[0] = closestDist;
		output[1] = closestEnemy.x;
		output[2] = closestEnemy.y;		
		return output;
	}

	
	private void microCode() throws GameActionException {
		Robot[] enemiesList = rc.senseNearbyGameObjects(Robot.class, 100000, rc.getTeam().opponent());
		int[] closestEnemyInfo = getClosestEnemy(enemiesList);
		MapLocation closestEnemyLocation = new MapLocation(closestEnemyInfo[1], closestEnemyInfo[2]);
		if (rc.senseNearbyGameObjects(Robot.class, 14, rc.getTeam().opponent()).length > 0) {
			double[] our23 = getEnemies2Or3StepsAway();
			double[] enemy23 = getEnemies2Or3StepsAwaySquare(closestEnemyLocation, rc.getTeam().opponent());
			Direction dir = currentLocation.directionTo(closestEnemyLocation);
//			int numAlliesNext = getNumAlliedNeighborsSquare(currentLocation.add(dir));
//			int numAllies = getNumAlliedNeighbors();
			if (our23[0] + our23[1] < enemy23[0] + enemy23[1]) {
				NavSystem.goToLocation(closestEnemyLocation);
			} else if (our23[0] + our23[1] > enemy23[0] + enemy23[1]){
				NavSystem.goAwayFromLocation(closestEnemyLocation);
			}
		} else {
			NavSystem.goToLocation(closestEnemyLocation);
		}
	}
	
	
	/** code to be used by capturers
	 * 
	 * @throws GameActionException
	 */
	private void captureCode() throws GameActionException {
		if (!unassigned) { // if assigned to something
			EncampmentJobSystem.updateJobTaken(assignedChannel);
		}
		if (rc.isActive()) {
			if (rc.senseEncampmentSquare(currentLocation) && currentLocation.equals(goalLoc)) {
				rc.captureEncampment(RobotType.GENERATOR);
			} else {
				if (NavSystem.navMode == NavMode.BFSMODE) {
					NavSystem.tryBFSNextTurn();
				} else if (NavSystem.navMode == NavMode.GETCLOSER){
					NavSystem.tryMoveCloser();
				} else if (rc.getLocation().distanceSquaredTo(goalLoc) <= 8) {
					NavSystem.setupGetCloser(goalLoc);
					NavSystem.tryMoveCloser();
				} else {
					NavSystem.goToLocation(goalLoc);
//					if (NavSystem.navMode == NavMode.NEUTRAL){
//						NavSystem.setupSmartNav(goalLoc);
//						NavSystem.followWaypoints();
//					} else {
//						NavSystem.followWaypoints();
//					}
				}
					
			}
			
		}
	}
	/**
	 * Given a center MapLocation and a radiusSquared, returns true if the circle is densely packed with allied mines.
	 * @param center
	 * @param radiusSquared
	 * @return
	 */
	private boolean minesDenselyPacked(MapLocation center, int radiusSquared) {
		return rc.senseMineLocations(center, radiusSquared, rc.getTeam()).length >= (int)(2 * radiusSquared);
	}
	
	private static void print2Darray(int[][] array) {
		for (int i=0; i<5; i++) {
			System.out.println("Array:");
			System.out.println(array[i][0] + " " + array[i][1] + array[i][2] + " " + array[i][3] + " " + array[i][4]);
		}
	}
	
	private MapLocation findRallyPoint() {
		MapLocation enemyLoc = EnemyHQLocation;
		MapLocation ourLoc = HQLocation;
		int x, y;
		x = (enemyLoc.x+3*ourLoc.x)/4;
		y = (enemyLoc.y+3*ourLoc.y)/4;
		return new MapLocation(x,y);
	}

}
