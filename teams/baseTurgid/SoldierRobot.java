package baseTurgid;

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
	
	// When soldier is born, its state is NEW (might have to walk out of mines that are completely surrounding the base)
	public SoldierState soldierState = SoldierState.NEW;
	
	// For mining
//	private MapLocation miningCenter;
//	private int miningRadius;
//	private int miningRadiusSquared;
//	private int miningMaxRadius;
	
	// encampment workers
	public boolean unassigned = true;

	public MapLocation currentLocation;
	
	public MapLocation rallyPoint; 
	
	public ChannelType powerChannel = ChannelType.HQPOWERLEVEL;
	
	// for mining for nuke bots
	public Direction dirToEnemyHQ;	
	public int offset;
	public int newOffset;
	public int randInt;
	public MapLocation miningStartLocation;
	public Direction miningDirConstant;
	public Direction miningDirConstantOpp;
	public MapLocation miningDestination;
	
	// still for mining - variables describing the line from our hq to the enemy hq
	public double lineA, lineB, lineC, lineDistanceDenom;
	public int x1, y1, x2, y2; // coordinates of our hq and enemy hq
	
	public SoldierRobot(RobotController rc) throws GameActionException {
		super(rc);
		
		NavSystem.init(this);
		
		rallyPoint = findRallyPoint();
		
		ChannelType channel = EncampmentJobSystem.findJob();
		if (channel != null) {
			unassigned = false;
			EncampmentJobSystem.updateJobTaken();
		}
		
		initializeMining();
	}
	
	public void initializeMining() {
		x1 = DataCache.ourHQLocation.x;
		y1 = DataCache.ourHQLocation.y;
		x2 = DataCache.enemyHQLocation.x;
		y2 = DataCache.enemyHQLocation.y;
		
		lineA = (double)(y2-y1)/(x2-x1);
		lineB = -1;
		lineC = y1 - lineA * x1;
		lineDistanceDenom = Math.sqrt(lineA*lineA + lineB*lineB);
		
		dirToEnemyHQ = rc.getLocation().directionTo(DataCache.enemyHQLocation);
		if (Util.randInt() % 2 == 0) {
			miningDirConstant = dirToEnemyHQ.rotateLeft().rotateLeft();
			miningDirConstantOpp = dirToEnemyHQ.rotateRight().rotateRight();
		} else {
			miningDirConstantOpp = dirToEnemyHQ.rotateLeft().rotateLeft();
			miningDirConstant = dirToEnemyHQ.rotateRight().rotateRight();
		}
		
		if (strategy == Strategy.NUKE) {
			if (Clock.getRoundNum() < 10 * 3) {
				offset = 2;
			} else if (Clock.getRoundNum() < 10 * 6) {
				offset = 2;
			} else {
				offset = 3;
			}
		} else {
			if (Clock.getRoundNum() < 10 * 3) {
				offset = 2;
			} else if (Clock.getRoundNum() < 10 * 6) {
				offset = 3;
			} else {
				offset = 4;
			}
		}
		
		newOffset = 2 * offset + 1;
		randInt = (Util.randInt() % newOffset);
		if (randInt < 0) {
			randInt += newOffset;
		}
		randInt -= offset;
		miningStartLocation = DataCache.ourHQLocation.add(miningDirConstant, randInt);
		int newX = miningStartLocation.x;
		int newY = miningStartLocation.y;
		// make sure the location doesn't fall off the map
		if (newX < 0) {
			newX = 0;
		} else if (newX >= DataCache.mapWidth) {
			newX = DataCache.mapWidth - 1;
		}
		if (newY < 0) {
			newY = 0;
		} else if (newY >= DataCache.mapHeight){
			newY = DataCache.mapHeight - 1;
		}
		miningStartLocation = new MapLocation(newX, newY);
		miningDestination = DataCache.enemyHQLocation;
	}
	
	private MapLocation findRallyPoint() {
		MapLocation enemyLoc = DataCache.enemyHQLocation;
		MapLocation ourLoc = DataCache.ourHQLocation;
		int x, y;
		x = (enemyLoc.x + 3 * ourLoc.x) / 4;
		y = (enemyLoc.y + 3 * ourLoc.y) / 4;
		return new MapLocation(x,y);
	}
	
	public double distanceToLine(MapLocation location) {
		int x = location.x;
		int y = location.y;
		return Math.abs(lineA * x + lineB * y + lineC) / lineDistanceDenom;
	}	
	
	@Override
	public void run() {
		try {
			DataCache.updateRoundVariables();
			currentLocation = rc.getLocation(); // LEAVE THIS HERE UNDER ALL CIRCUMSTANCES
			
			if (unassigned) {
				// If this is not an encampment worker
				// Check if our nuke is half done
				if (!ourNukeHalfDone) {
					Message message = BroadcastSystem.read(ChannelType.OUR_NUKE_HALF_DONE);
					if (message.isValid && message.body == 1) {
						ourNukeHalfDone = true;
					}
				}
				// Check if enemy nuke is half done
				if (!enemyNukeHalfDone) {
					Message message = BroadcastSystem.read(ChannelType.ENEMY_NUKE_HALF_DONE);
					if (message.isValid && message.body == 1) {
						enemyNukeHalfDone = true;
					}
				}
				if (enemyNukeHalfDone && !ourNukeHalfDone) {
					soldierState = SoldierState.ALL_IN;
				}
				
				// if we're new
				if (soldierState == SoldierState.NEW) {
					// If we're standing on a mine close to our base, we should clear out the mine
					Team mineTeam = rc.senseMine(rc.getLocation());
					if (mineTeam != null && mineTeam != rc.getTeam()) {
						soldierState = SoldierState.ESCAPE_HQ_MINES;
					} else {
//						if (strategy == Strategy.NUKE) {
//							soldierState = SoldierState.FINDING_START_MINE_POSITIONS;
//						} else {
//							soldierState = SoldierState.RALLYING;
//						}
						soldierState = SoldierState.FINDING_START_MINE_POSITIONS;
					}
				}
				
				switch (soldierState) {
				// FOR THE BEGINNING, WHEN WE FIND THE STARTING MINING LOCATIONS
				case FINDING_START_MINE_POSITIONS:
					if (DataCache.numEnemyRobots == 0) {
						int distanceSquaredToMiningStartLocation = rc.getLocation().distanceSquaredTo(miningStartLocation);
						if (distanceSquaredToMiningStartLocation == 0 ||
								(distanceSquaredToMiningStartLocation <= 2 && miningStartLocation.equals(DataCache.ourHQLocation))) {
							soldierState = SoldierState.MINING;
							break;
							// TODO: fall through?
						} else if (distanceSquaredToMiningStartLocation <= 2 && rc.senseEncampmentSquares(miningStartLocation, 0, null).length == 1) {
							// Choose another miningStartLocation
							miningStartLocation = DataCache.ourHQLocation.add(miningDirConstant, (randInt + 1) % offset);;
						} else {
							Direction dir = rc.getLocation().directionTo(miningStartLocation);
							NavSystem.goDirectionAndDefuse(dir);
							break;
						}
					} else {
						soldierState = SoldierState.FIGHTING;
						break;
						// TOOD: fall through?
					}
				case MINING:
					int hqPowerLevel = Integer.MAX_VALUE;
					Message message = BroadcastSystem.read(powerChannel);
					if (message.isValid) {
						hqPowerLevel = message.body;
					} else {
						hqPowerLevel = (int) rc.getTeamPower();
					}
					
					if (DataCache.numEnemyRobots > 0) {
						soldierState = SoldierState.FIGHTING;
					} else if (hqPowerLevel < 10*(1+DataCache.numAlliedEncampments) ) {
						soldierState = SoldierState.PUSHING;
					} else {
						mineCode();
					}
					break;
				// FOR ESCAPING MINES IF THEY SURROUND THE HQ
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
						if (strategy == Strategy.NUKE) { 
							soldierState = SoldierState.FINDING_START_MINE_POSITIONS;
						} else {
							soldierState = SoldierState.RALLYING;
						}
					}
					break;
				case ALL_IN:
					if (DataCache.numEnemyRobots > 0) {
						aggressiveMicroCode();
					} else{
						pushCodeGetCloser();
					}
					break;
				case PUSHING: 
					if (DataCache.numEnemyRobots > 0) {
						soldierState = SoldierState.FIGHTING;
					} else {
						pushCodeSmart();
					}
				case FIGHTING:
					if (DataCache.numEnemyRobots == 0) {
						if (DataCache.numAlliedSoldiers < Constants.FIGHTING_NOT_ENOUGH_ALLIED_SOLDIERS) {
							soldierState = SoldierState.RALLYING;
						} else {
							soldierState = SoldierState.PUSHING;
						}
					} else {
						// Otherwise, just keep fighting
						defendMicro();
//						microCode();
					}
					break;
				case RALLYING:
					int hqPowerLevel2 = Integer.MAX_VALUE;
					Message message2 = BroadcastSystem.read(powerChannel);
					if (message2.isValid) {
						hqPowerLevel2 = message2.body;
					} else {
						hqPowerLevel2 = (int) rc.getTeamPower();
					}

					// If there are enemies nearby, trigger FIGHTING SoldierState
					if (DataCache.numEnemyRobots > 0) {
						soldierState = SoldierState.FIGHTING;
					} else if (hqPowerLevel2 < 10*(1+DataCache.numAlliedEncampments) ) {
						soldierState = SoldierState.PUSHING;
					} else {
						boolean layedMine = false;
						if (rc.senseMine(currentLocation) == null) {
							if (rc.isActive() && Util.Random() < 0.1) {
								rc.layMine();
								layedMine = true;
							}
						} 
						if (!layedMine) {
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
	
	private void defendMicro() throws GameActionException {
		Robot[] enemiesList = rc.senseNearbyGameObjects(Robot.class, 100000, rc.getTeam().opponent());
		int[] closestEnemyInfo = getClosestEnemy(enemiesList);
		MapLocation closestEnemyLocation = new MapLocation(closestEnemyInfo[1], closestEnemyInfo[2]);
		double[][] activites = enemyActivites (currentLocation, rc.getTeam());
		if (activites[2][0]+activites[1][0]+activites[0][0] == 0){
			NavSystem.goToLocationAvoidMines(closestEnemyLocation);
			rc.setIndicatorString(0, "Forward1");
		}
		if (!minedUpAndReadyToGo(currentLocation)){
			if (activites[1][0]+activites[0][0]>2) {
//			if (activites[1][0]>1 || activites[0][0] == 0) {
				NavSystem.goToLocationAvoidMines(DataCache.ourHQLocation);
				rc.setIndicatorString(0, "Back1");
			}
		} else {
			MapLocation bestLocation = currentLocation;
			int positionQuality=-1000;
			MapLocation newPosition;
			int value;
			for (int i = 8; --i >= 0; ) {
				newPosition = currentLocation.add(Direction.values()[i]);
				value = positionValue(newPosition);
				if (value>positionQuality) {
					positionQuality=value;
					bestLocation=newPosition;
					break;
				}
			}
			NavSystem.goToLocationAvoidMines(bestLocation);
			rc.setIndicatorString(0, "Pos");
		}
	}
	
	private int positionValue(MapLocation location) throws GameActionException {
		int points=0;
		double[][] acts = enemyActivites(location, rc.getTeam());
		if (!minedUpAndReadyToGo(location)){
			if (acts[1][0]+acts[0][0]>0) {
				points -= 2;
//				points -= 2*(acts[0][0]+acts[1][0]);
			}
			if (acts[0][1]!=0) {
				points+=6*acts[0][1];
//				points+=3*acts[0][1];
			}
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
		for (int i = enemiesInVision.length; --i >= 0; ) {
			Robot enemy = enemiesInVision[i];
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
		for (int i = 8; --i >= 0; ) {
			surrounded = (rc.getTeam() == rc.senseMine(location.add(DataCache.directionArray[i])));
			if (surrounded == false){
				break;
			}
		}
		return surrounded;
	}
	
	public void mineCode() throws GameActionException {
		if (DataCache.numEnemyRobots == 0) {
			// Do mining
			if (rc.hasUpgrade(Upgrade.PICKAXE)) {
				if (rc.isActive()) {
					int x = rc.getLocation().x;
					int y = rc.getLocation().y;
					if ((2 * x + y) % 5 == 0 && rc.senseMine(rc.getLocation()) == null) {
						rc.layMine();
					} else {
//						if (NavSystem.navMode == NavMode.NEUTRAL) {
//							NavSystem.setupMiningNav();
//						} else {
//							NavSystem.followWaypoints(true, false);
//						}
						MapLocation newLocation1 = rc.getLocation().add(miningDirConstant);
						MapLocation newLocation2 = rc.getLocation().add(miningDirConstantOpp);
						if (distanceToLine(newLocation1) <= 4 && rc.senseMine(newLocation1) == null) {
							NavSystem.goDirectionAndDefuse(miningDirConstant);
						} else if (distanceToLine(newLocation2) <= 4 && rc.senseMine(newLocation2) == null) {
							NavSystem.goDirectionAndDefuse(miningDirConstantOpp);
						}
						
						Direction dir = rc.getLocation().directionTo(miningDestination);
						NavSystem.goDirectionAndDefuse(dir);
					}
				}
			} else if (rc.getLocation().distanceSquaredTo(DataCache.ourHQLocation) <= 121) {
				// no upgrade
				if (rc.isActive()) {
					if (rc.senseMine(rc.getLocation()) == null) {
						rc.layMine();
					} else {
//						if (NavSystem.navMode == NavMode.NEUTRAL) {
//							NavSystem.setupMiningNav();
//						} else {
//							NavSystem.followWaypoints(true, false);
//						}
						// check to see if mines on left or right are untaken
						MapLocation newLocation1 = rc.getLocation().add(miningDirConstant);
						MapLocation newLocation2 = rc.getLocation().add(miningDirConstantOpp);
						if (distanceToLine(newLocation1) <= 4 && rc.senseMine(newLocation1) == null) {
							NavSystem.goDirectionAndDefuse(miningDirConstant);
						} else if (distanceToLine(newLocation2) <= 4 && rc.senseMine(newLocation2) == null) {
							NavSystem.goDirectionAndDefuse(miningDirConstantOpp);
						}
						
						Direction dir = rc.getLocation().directionTo(miningDestination);
						NavSystem.goDirectionAndDefuse(dir);
					}
				}
			}
		} else {
			// There are enemies, so ditch mining and go defend
			soldierState = SoldierState.FIGHTING;
		}
	}

	public void microCode() throws GameActionException {
		Robot[] enemiesList = rc.senseNearbyGameObjects(Robot.class, 100000, rc.getTeam().opponent());
		int[] closestEnemyInfo = getClosestEnemy(enemiesList);
		MapLocation closestEnemyLocation = new MapLocation(closestEnemyInfo[1], closestEnemyInfo[2]);
		int enemyDistSquared = closestEnemyLocation.distanceSquaredTo(rc.getLocation());

		if (enemyDistSquared <= 2) { // if there is enemy in one dist
//			Robot[] enemiesInOneDist = rc.senseNearbyGameObjects(Robot.class, 2, rc.getTeam().opponent());
			// stay
//			rc.setIndicatorString(0, "stay1");
		} else if (enemyDistSquared == 16 || enemyDistSquared > 18){ // if no enemies in one, two, or three dist
//			rc.setIndicatorString(0, "no enemies in 1,2,3 dist");
//			rc.setIndicatorString(1, Integer.toString(DataCache.numNearbyEnemySoldiers));
			if (DataCache.numNearbyEnemySoldiers == 0 || DataCache.numNearbyAlliedSoldiers >= 3 * DataCache.numNearbyEnemySoldiers) { 
				// if no enemies in 5-dist or we outnumber them 5 to 1
				NavSystem.goToLocation(closestEnemyLocation);
			} else {
				NavSystem.goToLocationAvoidMines(closestEnemyLocation);
			}
		} else { // enemies in two or three dist
			double[] our23 = getEnemies2Or3StepsAway();
			double[] enemy23 = getEnemies2Or3StepsAwaySquare(closestEnemyLocation, rc.getTeam().opponent());
//			rc.setIndicatorString(2, our23[0] + " " + our23[1] + " " + our23[2]);
//
//			rc.setIndicatorString(1, enemy23[0] + " " + enemy23[1] + " " + enemy23[2]);
			if (our23[1] > 0) { // closest enemy in 2 dist


				if (enemy23[0] > 0) { // if enemy has dist 1
					NavSystem.goToLocationAvoidMines(closestEnemyLocation);
//					rc.setIndicatorString(0, "forward2");
				} else {
					if (enemy23[1] + enemy23[0] > our23[1] + our23[2]+1) {
						// move forward
						NavSystem.goToLocationAvoidMines(closestEnemyLocation);
//						rc.setIndicatorString(0, "forward2");
					} 
					//        			else if (enemy23[0] + enemy23[1] + enemy23[2] > our23[1] + our23[2]+2) {
					//                		rc.setIndicatorString(0, "back2.5");
					//                		goAwayFromLocation(closestEnemyLocation);
					//                		//back
					//                	} 
					else {
						NavSystem.goAwayFromLocationAvoidMines(closestEnemyLocation);
//						rc.setIndicatorString(0, "back2");
					}
				}

			} else { // closest enemy is 3 dist
				if (enemy23[0] > 0) {
					NavSystem.goToLocationAvoidMines(closestEnemyLocation);
//					rc.setIndicatorString(0, "forward4");
				} else if (enemy23[1] > 0) { // if enemy 2dist is > 0
					int closestDist = 100;
					int dist;
					MapLocation closestAllyLocation = null;
					Robot[] twoDistAllies = rc.senseNearbyGameObjects(Robot.class, closestEnemyLocation, 8, rc.getTeam());
					for (int i = twoDistAllies.length; --i >= 0; ) {
						Robot ally = twoDistAllies[i];
						RobotInfo arobotInfo = rc.senseRobotInfo(ally);
						dist = arobotInfo.location.distanceSquaredTo(rc.getLocation());
						if (dist<closestDist){
							closestDist = dist;
							closestAllyLocation = arobotInfo.location;
						}
					}

					double[] ally23 = getEnemies2Or3StepsAwaySquare(closestAllyLocation, rc.getTeam());

					if (enemy23[0] + enemy23[1] + enemy23[2] > ally23[1] + ally23[2]) {
						NavSystem.goToLocationAvoidMines(closestEnemyLocation);
//						rc.setIndicatorString(0, "forward3");
					} else {
//						rc.setIndicatorString(0, "stay3");
					}
				} else {
					if (enemy23[2] >= 7) {
						NavSystem.goToLocationAvoidMines(closestEnemyLocation);
					}
					// otherwise, stay
				}
			}
		}
	}



	private double[] getEnemies2Or3StepsAway() throws GameActionException {
		double count1 = 0;
		double count2 = 0;
		double count3 = 0;
		Robot[] enemiesInVision = rc.senseNearbyGameObjects(Robot.class, 18, rc.getTeam().opponent());
		for (int i = enemiesInVision.length; --i >= 0; ) {
			Robot enemy = enemiesInVision[i];
			RobotInfo rinfo = rc.senseRobotInfo(enemy);
			int dist = rinfo.location.distanceSquaredTo(rc.getLocation());
			if (rinfo.type == RobotType.SOLDIER) {
				if (dist <= 2) {
					count1++;
				} else if (dist <=8) {
					count2++;
				} else if (dist > 8 && (dist <= 14 || dist == 18)) {
					count3++;
				}
			} else {
				if (dist <= 2) {
					count1++;
				} else if (dist <=8) {
					count2 += 1;
				} else if (dist > 8 && (dist <= 14 || dist == 18)) {
					count3 += 1;
				}
			}
		}

		double[] output = {count1, count2, count3};
		return output;
	}

	private double[] getEnemies2Or3StepsAwaySquare(MapLocation square, Team squareTeam) throws GameActionException {
		double count1 = 0;
		double count2 = 0;
		double count3 = 0;
		Robot[] enemiesInVision = rc.senseNearbyGameObjects(Robot.class, square, 18, squareTeam.opponent());
		for (int i = enemiesInVision.length; --i >= 0; ) {
			Robot enemy = enemiesInVision[i];
			RobotInfo rinfo = rc.senseRobotInfo(enemy);
			int dist = rinfo.location.distanceSquaredTo(square);
			if (rinfo.type == RobotType.SOLDIER) {
				if (dist <= 2) {
					count1++;
				} else if (dist <=8) {
					count2++;
				} else if (dist <= 14 || dist == 18) {
					count3++;
				}
			} else {
				if (dist <= 2) {
					count1++;
				} else if (dist <=8) {
					count2 += 1;
				} else if (dist <= 14 || dist == 18) {
					count3 += 1;
				}
			}
		}

		int selfDist = square.distanceSquaredTo(rc.getLocation());
		
		if (selfDist <= 2) {
			count1++;
		} else if (selfDist <= 8) {
			count2++;
		} else if (selfDist <= 14 || selfDist == 18) {
			count3++;
		}

		double[] output = {count1, count2, count3};
		return output;
	}

	public void aggressiveMicroCode() throws GameActionException {
		Robot[] enemiesList = rc.senseNearbyGameObjects(Robot.class, 50, rc.getTeam().opponent());
		int[] closestEnemyInfo = getClosestEnemy(enemiesList);
		MapLocation closestEnemyLocation = new MapLocation(closestEnemyInfo[1], closestEnemyInfo[2]);
		
		if (DataCache.numNearbyAlliedSoldiers > 1.5 * DataCache.numNearbyEnemyRobots) {
			NavSystem.goToLocation(closestEnemyLocation);
		} else {
			microCode();
		}
	}
	
	public int[] getClosestEnemy(Robot[] enemyRobots) throws GameActionException {
		int closestDist = rc.getLocation().distanceSquaredTo(rc.senseEnemyHQLocation());
		MapLocation closestEnemy=rc.senseEnemyHQLocation(); // default to HQ

		int dist = 0;
		for (int i = enemyRobots.length; --i >= 0; ) {
			RobotInfo arobotInfo = rc.senseRobotInfo(enemyRobots[i]);
			dist = arobotInfo.location.distanceSquaredTo(rc.getLocation());
			if (dist < closestDist){
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
	
	/**
	 * Push code that uses getcloser nav
	 * @throws GameActionException
	 */
	public void pushCodeGetCloser() throws GameActionException {
		if (NavSystem.navMode != NavMode.GETCLOSER || NavSystem.destination != DataCache.enemyHQLocation) {
			NavSystem.setupGetCloser(DataCache.enemyHQLocation);
		}
		if (rc.isActive()) {
			NavSystem.moveCloserFavorNoMines();
		}
	}
	
	/**
	 * Push code that uses smart nav
	 * @throws GameActionException
	 */
	public void pushCodeSmart() throws GameActionException {		
		if (NavSystem.navMode != NavMode.SMART || NavSystem.destination != DataCache.enemyHQLocation) {
			NavSystem.setupSmartNav(DataCache.enemyHQLocation);
		}
		NavSystem.followWaypoints(true, true);
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
	 * Code below this line is for circle mining
	 */
	
//	public MapLocation calculateMiningOffsetCenter() {
//		int ourX = DataCache.ourHQLocation.x;
//		int ourY = DataCache.ourHQLocation.y;
//		int enemyX = DataCache.enemyHQLocation.x;
//		int enemyY = DataCache.enemyHQLocation.y;
//		return new MapLocation((4 * ourX + enemyX) / 5, (4 * ourY + enemyY) / 5);
//	}
//	
//	public int calculateMiningCenterRadiusSquared() {
//		return DataCache.ourHQLocation.distanceSquaredTo(DataCache.enemyHQLocation) / (5 * 5);
//	}
//	
//	/**
//	 * Given a center MapLocation and a radiusSquared, returns true if the circle is densely packed with allied mines.
//	 * @param center
//	 * @param radiusSquared
//	 * @return
//	 */
//	private boolean minesDenselyPacked(MapLocation center, int radiusSquared) {
//		return rc.senseMineLocations(center, radiusSquared, rc.getTeam()).length >= (int)(2 * radiusSquared);
//	}
//	
//	private static void print2Darray(int[][] array) {
//		for (int i=0; i<5; i++) {
//			System.out.println("Array:");
//			System.out.println(array[i][0] + " " + array[i][1] + array[i][2] + " " + array[i][3] + " " + array[i][4]);
//		}
//	}
//	
//	/**
//	 * Set up a center MapLocation for mining in a circle
//	 * @param center
//	 */
//	private void setupCircleMining(MapLocation center, int maxRadius) {
////		soldierState = SoldierState.MINING_IN_CIRCLE;
//		miningCenter = center;
//		miningMaxRadius = maxRadius;
//		miningRadius = Constants.INITIAL_MINING_RADIUS;
//		miningRadiusSquared = miningRadius * miningRadius;
//	}
//	
//	/**
//	 * This method tells the soldier to mine in a circle (as set up by setupCircleMining())
//	 * @return true if we can still mine, and false if the circle radius has exceeded the maxMiningRadius
//	 * @throws GameActionException
//	 */
//	private boolean mineInCircle() throws GameActionException {
////		rc.setIndicatorString(0, "miningRadiusSquared " + miningRadiusSquared);
//		if (rc.isActive()) {
//			if (minesDenselyPacked(miningCenter, miningRadiusSquared)) {
//				// mines are fairly dense, so expand the circle in which to mine
//				miningRadius += Constants.MINING_RADIUS_DELTA;
//				if (miningRadius > miningMaxRadius) {
//					return false;
//				}
//				miningRadiusSquared = miningRadius * miningRadius;
//			}
//			if (rc.getLocation().distanceSquaredTo(miningCenter) >= miningRadiusSquared) {
//				// If we're too far from the center, move closer
//				NavSystem.goToLocation(miningCenter);
//			} else if (rc.getLocation().distanceSquaredTo(miningCenter) <= Math.pow(miningRadius - Constants.MINING_CIRCLE_DR_TOLERANCE, 2)) {
//				// If we're too close to the center, move away
//				NavSystem.goDirectionAndDefuse(rc.getLocation().directionTo(miningCenter).opposite());
//			} else {
//				// Lay a mine if possible
//				if (rc.senseMine(rc.getLocation()) == null) {
//					rc.layMine();
//				}
//				// Walk around the circle
//				Direction dir = rc.getLocation().directionTo(miningCenter).rotateLeft().rotateLeft(); // move counterclockwise around circle
//				NavSystem.goDirectionAndDefuse(dir);
//			}
//		}
//		return true;
//	}
//	
//	private int getNumAlliedNeighbors() {
//		return rc.senseNearbyGameObjects(Robot.class, 2, rc.getTeam()).length;
//	}
//	
//	private int getNumAlliedNeighborsSquare(MapLocation square) {
//		return rc.senseNearbyGameObjects(Robot.class, square, 2, rc.getTeam()).length;
//	}

}
