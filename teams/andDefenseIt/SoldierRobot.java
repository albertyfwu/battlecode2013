package andDefenseIt;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Upgrade;

public class SoldierRobot extends BaseRobot {
	
	public Platoon platoon;
	
	public SoldierState soldierState = SoldierState.NEW;
	
	// For mining
	private MapLocation miningCenter;
	private int miningRadius;
	private int miningRadiusSquared;
	private int miningMaxRadius;
	
	public boolean unassigned = true;
	
	public MapLocation currentLocation;
	
	public MapLocation rallyPoint;
	
	public ChannelType powerChannel = ChannelType.HQPOWERLEVEL;
	
	public SoldierRobot(RobotController rc) throws GameActionException {
		super(rc);
		
		NavSystem.init(this);
		
		if (Clock.getRoundNum() > 120 && Clock.getRoundNum() < 150) {
			for (ChannelType channel: EncampmentJobSystem.encampmentJobChannelList) {
				Message message = BroadcastSystem.read(channel);
				if (message.isValid && message.body != 0xFFFFFF) {
					System.out.println("body: " + message.body);
				}
			}
		}

		rallyPoint = findRallyPoint();
		
//		ChannelType channel = EncampmentJobSystem.findJob();
//		if (channel != null) {
//			unassigned = false;
//			EncampmentJobSystem.updateJobTaken();
//		}

		// Set up mining in a circle?
//		setupCircleMining(rallyPoint, 10);
//		soldierState = SoldierState.RALLYING;
	}
	
	@Override
	public void run() {
		try {
			
			DataCache.updateRoundVariables();
			currentLocation = rc.getLocation(); // LEAVE THIS HERE UNDER ALL CIRCUMSTANCES
			if (unassigned) {
				
				// Check if enemy nuke is half done
				if (!enemyNukeHalfDone) {
					Message message = BroadcastSystem.read(ChannelType.ENEMY_NUKE_HALF_DONE);
					if (message.isValid && message.body == 1) {
						enemyNukeHalfDone = true;
					}
				}
				if (enemyNukeHalfDone) {
					soldierState = SoldierState.ALL_IN;
				}
				
				if (soldierState == SoldierState.NEW) {
					// If we're standing on a mine close to our base, we should clear out the mine
					Team mineTeam = rc.senseMine(rc.getLocation());
					if (mineTeam != null && mineTeam != rc.getTeam()) {
						soldierState = SoldierState.ESCAPE_HQ_MINES;
					} else {
//						setupCircleMining(DataCache.ourHQLocation, 20);
						soldierState = SoldierState.PLANT_MINES;
					}
				}
				
				rc.setIndicatorString(0, soldierState.toString());
				
				switch (soldierState) {
				case ESCAPE_HQ_MINES:
					// We need to run away from the mines surrounding our base
					Team mineTeam = rc.senseMine(rc.getLocation());
					if (mineTeam != null && mineTeam != rc.getTeam()) {
						// We need to run away from the mines surrounding our base
						if (NavSystem.safeLocationAwayFromHQMines != null) {
							NavSystem.goToLocationDontDefuseOrAvoidMines(NavSystem.safeLocationAwayFromHQMines);
						} else {
							NavSystem.goAwayFromHQEscapeMines(DataCache.ourHQLocation);
						}
					} else {
						// No more mines, so clear out HQ mines
						soldierState = SoldierState.CLEAR_OUT_HQ_MINES;
					}
					break;
				case CLEAR_OUT_HQ_MINES:
					// Clear out a path to the HQ
					Team mineTeam1 = rc.senseMine(rc.getLocation());
					if (mineTeam1 == null || mineTeam1 == rc.getTeam()) {
						NavSystem.goToLocation(DataCache.ourHQLocation);
					} else {
						// We're done
						soldierState = SoldierState.PLANT_MINES;
					}
				case DEFEND_BASE:
					// 
					break;
				case PLANT_MINES:
					mineTowardsEnemyHQ();
					if (DataCache.numTotalEnemyRobots != 0) {
						soldierState = SoldierState.FIGHTING;
					}
					break;
				case ALL_IN:
					microCode();
					break;
				case FIGHTING:
					if (DataCache.numTotalEnemyRobots == 0) {
						if (DataCache.numAlliedSoldiers < Constants.FIGHTING_NOT_ENOUGH_ALLIED_SOLDIERS) {
							soldierState = SoldierState.RALLYING;
						} else {
							defendMicro();
						}
						// Otherwise, just keep fighting
					} else {
						defendMicro();
					}
					break;
				case RALLYING:
					int hqPowerLevel = 10000;
					Message message = BroadcastSystem.read(powerChannel);
					if (message.isValid) {
						hqPowerLevel = message.body;
					}


					// If there are enemies nearby, trigger FIGHTING SoldierState
					if (DataCache.numTotalEnemyRobots > 0) {
						soldierState = SoldierState.FIGHTING;
					} else if (hqPowerLevel < 100) {
						soldierState = SoldierState.FIGHTING;
					} else {
						if (rc.senseMine(currentLocation) == null) {
							if (rc.isActive() && Util.Random() < 0.1) {
								rc.layMine();
							}
						} else {
							NavSystem.goToLocation(rallyPoint);
						}
					}
					break;
				default:
					break;
				}
			} else {
				// This soldier has an encampment job, so it should go do that job
				captureCode();
			}
		} catch (Exception e) {
			System.out.println("caught exception before it killed us:");
			System.out.println(rc.getRobot().getID());
			e.printStackTrace();
		}
	}
	
	/**
	 * Mine towards the enemy HQ, defusing neutral mines along the way
	 * @throws GameActionException
	 */
	private void mineTowardsEnemyHQ() throws GameActionException {
		if (rc.isActive()) {
			if (rc.senseMine(rc.getLocation()) == null) {
				// If we're not standing on a mine
				if (rc.hasUpgrade(Upgrade.PICKAXE)) {
					int x = rc.getLocation().x;
					int y = rc.getLocation().y;
					if ((2 * x + y) % 5 == 0) {
						// also need to check that all the squares next to you are defused
						int[][] directionOffsets = new int[][] {{0,1},{1,0},{0,-1},{-1,0}};
						for (int[] directionOffset : directionOffsets) {
							MapLocation newLocation = rc.getLocation().add(directionOffset[0], directionOffset[1]);
							Team mineTeam1 = rc.senseMine(newLocation);
							if (mineTeam1 == Team.NEUTRAL || mineTeam1 == rc.getTeam().opponent()) {
								// Defuse that mine
								rc.defuseMine(newLocation);
								return;
							}
						}
						if (rc.isActive()) {
							rc.layMine();
						}
					}
				} else {
					rc.layMine();
				}
			}
			Direction dirToEnemyHQ = rc.getLocation().directionTo(DataCache.enemyHQLocation);
			MapLocation iterLocation = rc.getLocation().add(dirToEnemyHQ);
			Team mineTeam = rc.senseMine(iterLocation);
			if (mineTeam == null || mineTeam == rc.getTeam()) {
				// If there is no mine, go towards that location
				NavSystem.goToLocation(iterLocation);
			} else if (mineTeam == rc.getTeam().opponent() || mineTeam == Team.NEUTRAL) {
				// If mine is neutral or from the other team, just defuse it
				rc.defuseMine(iterLocation);
			}
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
//		soldierState = SoldierState.MINING_IN_CIRCLE;
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
//		rc.setIndicatorString(0, "miningRadiusSquared " + miningRadiusSquared);
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
		int closestDist = currentLocation.distanceSquaredTo(DataCache.enemyHQLocation);
		MapLocation closestEnemy=DataCache.enemyHQLocation; // default to HQ

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

	private void defendMicro() throws GameActionException {
		Robot[] enemiesList = rc.senseNearbyGameObjects(Robot.class, 100000, rc.getTeam().opponent());
		if (!minedUpAndReadyToGo(currentLocation)){
			double[][] activites = enemyActivites (currentLocation, rc.getTeam());
			if (activites[1][0]+activites[0][0]>0) {
				NavSystem.goToLocationAvoidMines(DataCache.ourHQLocation);
			} else {
				if (activites[2][0]==0){
					int[] closestEnemyInfo = getClosestEnemy(enemiesList);
					MapLocation closestEnemyLocation = new MapLocation(closestEnemyInfo[1], closestEnemyInfo[2]);
					NavSystem.goToLocationAvoidMines(closestEnemyLocation);
				} else {
					// stay and zone them
				}
			}
		} else {
			MapLocation bestLocation=currentLocation;
			int positionQuality=-1000;
			MapLocation newPosition;
			int value;
			for (int i=8;i!=0;i--){
				newPosition = currentLocation.add(Direction.values()[i]);
				value = positionValue(newPosition);
				if (value>positionQuality) {
					positionQuality=value;
					bestLocation=newPosition;
					break;
				}
			}
			NavSystem.goToLocationAvoidMines(bestLocation);
		}
	}
	
	private int positionValue(MapLocation location) throws GameActionException{
		int points=0;
		double[][] acts = enemyActivites(location, rc.getTeam());
		if (!minedUpAndReadyToGo(location)){
			if (acts[1][0]+acts[0][0]>0) {
				points -= 20;
			}
			if (acts[0][1]!=0) {
				points+=20;
			}
			if (acts[0][1]>2) {
				points+=30;
			}
		} else {
			points += 5*acts[1][0];
		}
		return points;
	}
	
	private double[][] enemyActivites (MapLocation square, Team squareTeam) throws GameActionException{
		double acount1 = 0;
		double acount2 = 0;
		double acount3 = 0;
		double icount1 = 0;
		double icount2 = 0;
		double icount3 = 0;
		Robot[] enemiesInVision = rc.senseNearbyGameObjects(Robot.class, 18, squareTeam.opponent());
		for (Robot enemy: enemiesInVision) {
			if (rc.senseRobotInfo(enemy).roundsUntilMovementIdle==0){
				RobotInfo rinfo = rc.senseRobotInfo(enemy);
				int dist = rinfo.location.distanceSquaredTo(currentLocation);
				if (dist<=2) {
					acount1++;
				} else if(dist <=8) {
					acount2++;
				} else if (dist > 8 && (dist <= 14 || dist == 18)) {
					acount3++;
				}
			} else {
				RobotInfo rinfo = rc.senseRobotInfo(enemy);
				int dist = rinfo.location.distanceSquaredTo(currentLocation);
				if (dist<=2) {
					icount1++;
				} else if(dist <=8) {
					icount2++;
				} else if (dist > 8 && (dist <= 14 || dist == 18)) {
					icount3++;
				}
			}
		}
		
		double[][] output = {{acount1, icount1}, {acount2, icount2}, {acount3, icount3}};
		return output;
	}
	
	private boolean minedUpAndReadyToGo(MapLocation location){
		boolean surrounded = true;
		for (int i=8;i!=0;i--){
			surrounded = (rc.getTeam() == rc.senseMine(location.add(Direction.values()[i])));
			if (surrounded == false){
				break;
			}
		}
		return surrounded;
	}
	
	private void microCode() throws GameActionException {
		if (rc.isActive()) {
			Robot[] enemiesList = rc.senseNearbyGameObjects(Robot.class, 100000, rc.getTeam().opponent());
			int[] closestEnemyInfo = getClosestEnemy(enemiesList);
			MapLocation closestEnemyLocation = new MapLocation(closestEnemyInfo[1], closestEnemyInfo[2]);
			NavSystem.setupSmartNav(closestEnemyLocation);
			if (DataCache.numAlliedSoldiers > 3 * DataCache.numTotalEnemyRobots) {
//				NavSystem.goToLocation(closestEnemyLocation);
				double random = Util.Random();
				System.out.println("random: " + random);
				if (random < 0.05) {
					NavSystem.followWaypoints(true);
				} else {
					NavSystem.followWaypoints(false);
				}
				
			} else {
				if (DataCache.numNearbyEnemyRobots > 0) {
					double[] our23 = getEnemies2Or3StepsAway();
					double[] enemy23 = getEnemies2Or3StepsAwaySquare(closestEnemyLocation, rc.getTeam().opponent());
					Direction dir = currentLocation.directionTo(closestEnemyLocation);
					//					int numAlliesNext = getNumAlliedNeighborsSquare(currentLocation.add(dir));
					//					int numAllies = getNumAlliedNeighbors();
					if (our23[0] + our23[1] < enemy23[0] + enemy23[1]) {
						NavSystem.followWaypoints(false);
					} else if (our23[0] + our23[1] > enemy23[0] + enemy23[1]){
						NavSystem.goAwayFromLocationAvoidMines(closestEnemyLocation);
					}

				} else {
					//				NavSystem.goToLocationAvoidMines(closestEnemyLocation);

					if (DataCache.numTotalEnemyRobots > 0) {
						if (DataCache.numAlliedSoldiers > 3 * DataCache.numTotalEnemyRobots) {
							double random = Util.Random();
							System.out.println("random: " + random);
							if (random < 0.05) {
								NavSystem.followWaypoints(true);
							} else {
								NavSystem.followWaypoints(false);
							}
						}
						NavSystem.followWaypoints(false);
					} else {
						NavSystem.followWaypoints(true);

					}
				}
			}
		}
	}

	
	/** code to be used by capturers
	 * 
	 * @throws GameActionException
	 */
	private void captureCode() throws GameActionException {
		if (!unassigned) { // if assigned to something
			EncampmentJobSystem.updateJobTaken();
		}
		if (rc.isActive()) {
			if (rc.senseEncampmentSquare(currentLocation) && currentLocation.equals(EncampmentJobSystem.goalLoc)) {
				if (rc.getTeamPower() > rc.senseCaptureCost()) {
					rc.captureEncampment(EncampmentJobSystem.assignedRobotType);
				}
			} else {
				if (rc.senseNearbyGameObjects(Robot.class, 14, rc.getTeam().opponent()).length > 0) {
					unassigned = true;
					soldierState = SoldierState.FIGHTING;
				}
				if (NavSystem.navMode == NavMode.BFSMODE) {
					NavSystem.tryBFSNextTurn();
				} else if (NavSystem.navMode == NavMode.GETCLOSER){
					NavSystem.tryMoveCloser();
				} else if (rc.getLocation().distanceSquaredTo(EncampmentJobSystem.goalLoc) <= 8) {
					NavSystem.setupGetCloser(EncampmentJobSystem.goalLoc);
					NavSystem.tryMoveCloser();
				} else {
					NavSystem.goToLocation(EncampmentJobSystem.goalLoc);
//					if (NavSystem.navMode == NavMode.NEUTRAL){
//						NavSystem.setupSmartNav(EncampmentJobSystem.goalLoc);
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
		MapLocation enemyLoc = DataCache.enemyHQLocation;
		MapLocation ourLoc = DataCache.ourHQLocation;
		int x, y;
		x = (enemyLoc.x+3*ourLoc.x)/4;
		y = (enemyLoc.y+3*ourLoc.y)/4;
		return new MapLocation(x,y);
	}

}
