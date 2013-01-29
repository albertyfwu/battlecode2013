package team162;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Upgrade;

public class SoldierRobot extends BaseRobot {
	
	public int move_out_round = 10000;
	
	// When soldier is born, its state is NEW (might have to walk out of mines that are completely surrounding the base)
	public SoldierState soldierState = SoldierState.NEW;
	public SoldierState nextSoldierState;
	
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
	public ChannelType genCountChannel = ChannelType.GEN_COUNT;
	
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
	
	public int rSquared;
	public double mineDensity; // density of mines
	public int shieldsCutoff; // the cutoff for the number of shields to get before leaving the shields encampment
	
	public MapLocation shieldLocation;
	public MapLocation shieldQueueLocation;
	
	public double healthLastTurn = 40;
	
	public SoldierRobot(RobotController rc) throws GameActionException {
		super(rc);
		
		NavSystem.init(this);
		
		ChannelType channel = EncampmentJobSystem.findJob();
		if (channel != null) {
			unassigned = false;
			EncampmentJobSystem.updateJobTaken();
		}
		
		// TODO: this will fuck up if we ever build artillery for non-nuke bots
		// LEARN THE STRATEGY
//		Message message = BroadcastSystem.read(ChannelType.STRATEGY);
//		if (message.isValid && message.body < Strategy.values().length && message.body >= 0) {
//			strategy = Strategy.values()[message.body];
//		} else {
//			// we couldn't read the strategy channel
//			MapLocation[] alliedEncampmentSquares = rc.senseAlliedEncampmentSquares();
//			if (alliedEncampmentSquares.length == 0) {
//				strategy = Strategy.ECON;
//			} else {
//				Robot robot = (Robot) rc.senseObjectAtLocation(alliedEncampmentSquares[0]);
//				RobotInfo robotInfo = rc.senseRobotInfo(robot);
//				if (robotInfo.type == RobotType.ARTILLERY) {
//					strategy = Strategy.NUKE;
//				} else {
//					strategy = Strategy.ECON;
//				}
//			}
//		}
		
		strategy = Strategy.ECON;
		
//		rc.setIndicatorString(2, strategy.toString());
		
		rallyPoint = findRallyPoint();
		
		rSquared = DataCache.rushDistSquared / 4;
		
		initializeMining(); // TODO: do we really need every soldier to do this?
		
		// If this soldier is just born, and we're in ALL_IN mode, then charge up at shields and then go to the fight
		if (move_out_round == 10000) {
			Message message = BroadcastSystem.read(ChannelType.MOVE_OUT);
			if (message.isValid) {
				move_out_round = message.body;
			}
		}
		
		if (Clock.getRoundNum() > move_out_round) {
			// if we're in all-in mode, then we're a reinforcement
			// since we're a reinforcement, we should go to the shields if it exists and then go to battle
			soldierState = SoldierState.EXPRESS_CHARGE_SHIELDS;
		}
	}
	
	
	@Override
	public void run() {
		try {
			rc.setIndicatorString(0, soldierState.toString());
//			rc.setIndicatorString(1, Integer.toString(move_out_round));
//			rc.setIndicatorString(2, Integer.toString(move_out_round));
			
			DataCache.updateRoundVariables();
			currentLocation = rc.getLocation(); // LEAVE THIS HERE UNDER ALL CIRCUMSTANCES
			
			if (move_out_round == 10000) {
				Message message = BroadcastSystem.read(ChannelType.MOVE_OUT);
				if (message.isValid) {
					move_out_round = message.body;
				}
			}
			
			if (unassigned) {
				
				// check if we need to retreat
				if (!shieldExists() && soldierState != SoldierState.ALL_IN) {
					Message retreatMsg = BroadcastSystem.read(ChannelType.RETREAT_CHANNEL);
					if (retreatMsg.isValid && retreatMsg.body == Constants.RETREAT) { 
//						rc.setIndicatorString(1, "shouldn't get here");
						soldierState = SoldierState.RETREAT;
						System.out.println("retreat");
					}
				}
				
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
				if (enemyNukeHalfDone && !ourNukeHalfDone && soldierState != SoldierState.ALL_IN && soldierState != SoldierState.EXPRESS_CHARGE_SHIELDS) {
//					soldierState = SoldierState.ALL_IN;
//					BroadcastSystem.write(ChannelType.MOVE_OUT, Clock.getRoundNum() + 100);
//					rc.setIndicatorString(1, "do we get in here");
					if (move_out_round <= Clock.getRoundNum()) {
						soldierState = SoldierState.ALL_IN;
					} else {
						soldierState = SoldierState.RALLYING;
					}
				}
				
//				rc.setIndicatorString(0, soldierState.toString());
				
				switch (soldierState) {
				case NEW:
					newCode();
					break;
				case FINDING_START_MINE_POSITIONS:
					findingStartMinePositionsCode();
					break;
				case CHARGE_SHIELDS:
					if (shieldExists()) {
						chargeShieldsCode();
					} else {
						nextSoldierState = SoldierState.RALLYING;
						rallyingCode();
					}
					break;
				case EXPRESS_CHARGE_SHIELDS:
					if (shieldExists()) {
						expressChargeShieldsCode();
					} else {
//						rc.setIndicatorString(1, "shouldn't get here");
						nextSoldierState = SoldierState.ALL_IN;
						allInCode();
					}
					break;
				case MINING:
					miningCode();
					break;
				case ESCAPE_HQ_MINES:
					escapeHQMinesCode();
					break;
				case CLEAR_OUT_HQ_MINES:
					clearOutHQMinesCode();
					break;
				case ALL_IN:
					allInCode();
					break;
				case PUSHING: 
					pushingCode();
					break;
				case FIGHTING:
					fightingCode();
					break;
				case RALLYING:
					rallyingCode();
					break;
				case RETREAT:
					retreatCode();
					break;
				default:
					break;
				}
			} else {
				// This soldier has an encampment job, so it should go do that job
				captureCode();
			}
			
			double currHealth = rc.getEnergon();
			
			
			boolean artillerySeen = reportArtillerySighting(currHealth);
			if (artillerySeen && !shieldExists()) { // if we see artillery and we're near their base, retreat
				if (currentLocation.distanceSquaredTo(DataCache.enemyHQLocation) < 0.16 * DataCache.rushDistSquared) {
					soldierState = SoldierState.RETREAT;
					BroadcastSystem.write(ChannelType.RETREAT_CHANNEL, Constants.RETREAT);
				}
			}
			
			healthLastTurn = currHealth;
			
			if (nextSoldierState != null) {
				soldierState = nextSoldierState;
				nextSoldierState = null; // clear the state for the next call of run() to use
			}
		} catch (Exception e) {
			System.out.println("caught exception before it killed us:");
			System.out.println(rc.getRobot().getID());
			e.printStackTrace();
		}
	}
	
	private MapLocation findMidPoint() {
		MapLocation enemyLoc = DataCache.enemyHQLocation;
		MapLocation ourLoc = DataCache.ourHQLocation;
		int x, y;
		x = (enemyLoc.x + ourLoc.x) / 2;
		y = (enemyLoc.y + ourLoc.y) / 2;
		return new MapLocation(x,y);
	}
	
	public void initializeMining() {
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
//		rc.setIndicatorString(2, "newX: " + newX + ", newY: " + newY);
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
		if (strategy == Strategy.NUKE) {
			int dx = DataCache.enemyHQLocation.x - DataCache.ourHQLocation.x;
			int dy = DataCache.enemyHQLocation.y - DataCache.ourHQLocation.y;
			
			double vectorMag = Math.sqrt(dx*dx + dy*dy);
			double dxNorm = dx/vectorMag;
			double dyNorm = dy/vectorMag;
			
			int centerx = (int) (DataCache.ourHQLocation.x + 8 * dxNorm);
			int centery = (int) (DataCache.ourHQLocation.y + 8 * dyNorm);
			
			return new MapLocation(centerx, centery);
		} else {
			MapLocation enemyLoc = DataCache.enemyHQLocation;
			MapLocation ourLoc = DataCache.ourHQLocation;
			int x, y;
			x = (enemyLoc.x + 3 * ourLoc.x) / 4;
			y = (enemyLoc.y + 3 * ourLoc.y) / 4;
			return new MapLocation(x,y);
		}
	}
	
	public double distanceToLine(MapLocation location) {
		int x = location.x;
		int y = location.y;
		return Math.abs(lineA * x + lineB * y + lineC) / lineDistanceDenom;
	}
	
	public void getNewMiningStartLocation() {
		MapLocation newLocation = DataCache.ourHQLocation.add(miningDirConstant, (randInt + 1) % offset);
		int newX = newLocation.x;
		int newY = newLocation.y;
//		rc.setIndicatorString(2, "newX: " + newX + ", newY: " + newY);
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
	}

	
	public void newCode() throws GameActionException {
		// If we're standing on a mine close to our base, we should clear out the mine
		Team mineTeam = rc.senseMine(rc.getLocation());
		if (mineTeam != null && mineTeam != rc.getTeam()) {
			nextSoldierState = SoldierState.ESCAPE_HQ_MINES;
			escapeHQMinesCode();
		} else {
//			nextSoldierState = SoldierState.FINDING_START_MINE_POSITIONS;
//			findingStartMinePositionsCode();
//			nextSoldierState = SoldierState.PUSHING;
//			pushingCode();
			if (shieldExists()) {
				nextSoldierState = SoldierState.CHARGE_SHIELDS;
				chargeShieldsCode();
			} else {
//				nextSoldierState = SoldierState.RALLYING;
//				rallyingCode();
				if (Clock.getRoundNum() < 50) {
					nextSoldierState = SoldierState.PUSHING;
					pushingCode();
				} else {
					nextSoldierState = SoldierState.RALLYING;
					rallyingCode();
				}
			}
		}
	}

	public void rallyingCode() throws GameActionException {
//		if (enemyNukeHalfDone && !ourNukeHalfDone ) {
////			nextSoldierState = SoldierState.ALL_IN;
////			allInCode();
//			// charge up
//			if (shieldExists()) {
//				nextSoldierState = SoldierState.CHARGE_SHIELDS;
//				chargeShieldsCode();
//			} else {
//				
//			}
////			} else {
////				nextSoldierState = SoldierState.ALL_IN;
////				allInCode();
////			}
//		} else {
//			int hqPowerLevel2 = Integer.MAX_VALUE;
//			Message message2 = BroadcastSystem.read(powerChannel);
//			if (message2.isValid) {
//				hqPowerLevel2 = message2.body;
//			} else {
//				hqPowerLevel2 = (int) rc.getTeamPower();
//			}
			
			int genCount = Integer.MAX_VALUE;
			Message message = BroadcastSystem.read(genCountChannel);
			if (message.isValid){
				genCount = message.body;
			}
	
			// If there are enemies nearby, trigger FIGHTING SoldierState
			if (DataCache.numEnemyRobots > 0) {
				nextSoldierState = SoldierState.FIGHTING;
				fightingCode();
//			} else if (hqPowerLevel2 < 10*(1+DataCache.numAlliedEncampments) || hqPowerLevel2 < 100) {
			} else if (DataCache.numAlliedRobots >= (40 + 10 * (genCount))/1.5 && !enemyNukeHalfDone) {
				nextSoldierState = SoldierState.PUSHING;
				pushingCode();
			} else {
				mineDensity = findMineDensity();
				shieldsCutoff = (int) (DataCache.rushDist + 3 * (mineDensity * DataCache.rushDist)) + 50;
				if (rc.getShields() < shieldsCutoff - 50 && shieldExists()) {
//					rc.setIndicatorString(2, "lower bound: " + (shieldsCutoff-50));
					// not enough shields
					nextSoldierState = SoldierState.CHARGE_SHIELDS;
					chargeShieldsCode();
				} else {
					if (rc.senseEncampmentSquare(currentLocation) && currentLocation.distanceSquaredTo(DataCache.enemyHQLocation) < 0.55 * DataCache.rushDistSquared ) {
						if (rc.getTeamPower() > rc.senseCaptureCost() && Util.Random() < 0.5 && rc.isActive()) {
							rc.captureEncampment(RobotType.ARTILLERY);
						}
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
					
				}
			}
//		}
	}
	
	public void retreatCode() throws GameActionException {
		if (DataCache.numEnemyRobots == 0) {
			nextSoldierState = SoldierState.RALLYING;
			BroadcastSystem.write(ChannelType.RETREAT_CHANNEL, 0);
			rallyingCode();
		} else if (DataCache.numAlliedSoldiers >= Constants.FIGHTING_NOT_ENOUGH_ALLIED_SOLDIERS) {
			nextSoldierState = SoldierState.FIGHTING;
			fightingCode();
		} else {
			NavSystem.moveCloserFavorNoMines(rallyPoint);
		}
	}

	public void fightingCode() throws GameActionException {
		if (DataCache.numEnemyRobots == 0) {
			if (DataCache.numAlliedSoldiers < Constants.FIGHTING_NOT_ENOUGH_ALLIED_SOLDIERS) {
				if (move_out_round <= Clock.getRoundNum()) {
					nextSoldierState = SoldierState.PUSHING;
					pushingCode();
				} else {
					Message msg;
					if (Clock.getRoundNum() % Constants.CHANNEL_CYCLE == 0 && Clock.getRoundNum() > 0) {
						msg = BroadcastSystem.readLastCycle(ChannelType.ARTILLERY_SEEN);
					} else {
						msg = BroadcastSystem.read(ChannelType.ARTILLERY_SEEN);
					}
					
					if (Clock.getRoundNum() >= 200 || msg.body == Constants.TRUE) {
						nextSoldierState = SoldierState.RALLYING;
						rallyingCode();
					} else {
						nextSoldierState = SoldierState.PUSHING;
						pushingCode();
					}
				}
			} else {
				nextSoldierState = SoldierState.PUSHING;
				pushingCode();
			}
		} else {
			// Otherwise, just keep fighting
			if (strategy == Strategy.NUKE) {
				defendMicro();
			} else {
				microCode();
			}
		}
	}

	public void pushingCode() throws GameActionException {
		if (DataCache.numEnemyRobots > 0) {
			nextSoldierState = SoldierState.FIGHTING;
			fightingCode();
		} else {
//			pushCodeSmart();
			pushCodeGetCloser();
		}
	}

	public void allInCode() throws GameActionException {
		if (DataCache.numNearbyAlliedRobots > 0) {
//			aggressiveMicroCode();
			microCode();
		} else {
//			int genCount = Integer.MAX_VALUE;
//			Message message = BroadcastSystem.read(genCountChannel);
//			if (message.isValid){
//				genCount = message.body;
//			}
			
//			if (DataCache.numAlliedSoldiers >= (40 + 10 * genCount)/1.5) {
				pushCodeGetCloser();
//			}
		}
	}

	public void clearOutHQMinesCode() throws GameActionException {
		// Clear out a path to the HQ
		Team mineTeam1 = rc.senseMine(rc.getLocation());
		if (mineTeam1 == null || mineTeam1 == rc.getTeam() && rc.getLocation().distanceSquaredTo(DataCache.ourHQLocation) > 2) {
			NavSystem.goToLocation(DataCache.ourHQLocation);
		} else {
			// We're done
			if (strategy == Strategy.NUKE) { 
				nextSoldierState = SoldierState.FINDING_START_MINE_POSITIONS;
				findingStartMinePositionsCode();
			} else {
				nextSoldierState = SoldierState.RALLYING;
				rallyingCode();
			}
		}
	}

	public void escapeHQMinesCode() throws GameActionException {
		// We need to run away from the mines surrounding our base
		Team mineTeam = rc.senseMine(rc.getLocation());
		if (mineTeam != null && mineTeam != rc.getTeam()) {
			// We need to run away from the mines surrounding our base
			if (NavSystem.safeLocationAwayFromHQMines != null) {
				NavSystem.goToLocationDontDefuseOrAvoidMines(NavSystem.safeLocationAwayFromHQMines);
			} else {
				NavSystem.goAwayFromLocationEscapeMines(DataCache.ourHQLocation);
			}
		} else {
			// No more mines, so clear out HQ mines
			nextSoldierState = SoldierState.CLEAR_OUT_HQ_MINES;
			clearOutHQMinesCode();
		}
	}

	public void miningCode() throws GameActionException {
		int hqPowerLevel = Integer.MAX_VALUE;
		Message message = BroadcastSystem.read(powerChannel);
		if (message.isValid) {
			hqPowerLevel = message.body;
		} else {
			hqPowerLevel = (int) rc.getTeamPower();
		}
		
		if (DataCache.numEnemyRobots > 0 && strategy != Strategy.NUKE) {
//			nextSoldierState = SoldierState.FIGHTING;
//			fightingCode();
			if (shieldExists()) {
				nextSoldierState = SoldierState.CHARGE_SHIELDS;
				chargeShieldsCode();
			} else {
//				nextSoldierState = SoldierState.RALLYING;
//				rallyingCode();
				if (Clock.getRoundNum() < 50) {
					nextSoldierState = SoldierState.PUSHING;
					pushingCode();
				} else {
					nextSoldierState = SoldierState.RALLYING;
					rallyingCode();
				}
			}
		} else if (strategy == Strategy.NUKE && DataCache.numEnemyRobots > 1) {
//			nextSoldierState = SoldierState.FIGHTING;
//			fightingCode();
			if (shieldExists()) {
				nextSoldierState = SoldierState.CHARGE_SHIELDS;
				chargeShieldsCode();
			} else {
//				nextSoldierState = SoldierState.RALLYING;
//				rallyingCode();
				if (Clock.getRoundNum() < 50) {
					nextSoldierState = SoldierState.PUSHING;
					pushingCode();
				} else {
					nextSoldierState = SoldierState.RALLYING;
					rallyingCode();
				}
			}
		} else if (strategy == Strategy.NUKE && DataCache.numEnemyRobots == 1) {
			Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class, 14, rc.getTeam().opponent());
			if (enemyRobots.length == 1) {
				RobotInfo rinfo = rc.senseRobotInfo(enemyRobots[0]);
				NavSystem.goToLocation(rinfo.location);
			} else {
				miningSubroutine();
			}
		} else if (strategy != Strategy.NUKE && (hqPowerLevel < 10*(1+DataCache.numAlliedEncampments) || hqPowerLevel < 100) ) {
//			nextSoldierState = SoldierState.PUSHING;
//			pushingCode();
			if (shieldExists()) {
				nextSoldierState = SoldierState.CHARGE_SHIELDS;
				chargeShieldsCode();
			}
		} else {
			miningSubroutine();
		}
	}

	public boolean shieldExists() throws GameActionException {
		shieldLocation = EncampmentJobSystem.readShieldLocation();
		if (shieldLocation != null && rc.canSenseSquare(shieldLocation)) {
			// looks like something exists at the location...
			GameObject object = rc.senseObjectAtLocation(shieldLocation);
			if (object != null && rc.senseRobotInfo((Robot) object).type == RobotType.SHIELDS) {
				// set the shieldQueueLocation						
				int dx = shieldLocation.x - DataCache.ourHQLocation.x;
				int dy = shieldLocation.y - DataCache.ourHQLocation.y;
				
				double maxDxDy = Math.max(Math.abs(dx), Math.abs(dy));
				
				double dxNorm = dx / maxDxDy;
				double dyNorm = dy / maxDxDy;
				
				int centerx = (int) (shieldLocation.x - 5 * dxNorm);
				int centery = (int) (shieldLocation.y - 5 * dyNorm);
				
				shieldQueueLocation = new MapLocation(centerx, centery);
				
				rallyPoint = shieldQueueLocation;
						
				return true;
			}
		}
		return false;
	}
	
	private void chargeShieldsCode() throws GameActionException {
		if (DataCache.numEnemyRobots > 0) {
			nextSoldierState = SoldierState.PUSHING;
			pushingCode();
		} else {
			int genCount = Integer.MAX_VALUE;
			Message message = BroadcastSystem.read(genCountChannel);
			if (message.isValid){
				genCount = message.body;
			}
			if (DataCache.numAlliedRobots >= (40 + 10 * (genCount))/1.5 && !enemyNukeHalfDone) {
				nextSoldierState = SoldierState.PUSHING;
				pushingCode();
			} else {
				//			if (!ourNukeHalfDone && enemyNukeHalfDone) {
				//				nextSoldierState = SoldierState.ALL_IN;
				//				allInCode();
				//			} else {
				// either charge up shields next to the shields encampment, or wait at another location until there is space		
				//			MapLocation shieldQueueLocation = rallyPoint;
				// find an empty space next to the shields encampment
				int distanceSquaredToShields = rc.getLocation().distanceSquaredTo(shieldLocation);
				if (distanceSquaredToShields > 2) {
					// not charging yet
					for (int i = 8; --i >= 0; ) {
						// Check to see if it's empty
						MapLocation iterLocation = shieldLocation.add(DataCache.directionArray[i]);
						if (rc.senseObjectAtLocation(iterLocation) == null) {
							// Oh, there's an empty space! let's go to it
							NavSystem.goToLocation(iterLocation);
							return;
						}
					}
					// we found the shields encampment, but there are no empty spaces, so wait at the queue location
					//					NavSystem.goToLocation(EncampmentJobSystem.shieldsQueueLoc);
					NavSystem.goToLocation(shieldQueueLocation);
				} else {
					// already charging
					mineDensity = findMineDensity();
					shieldsCutoff = (int) (DataCache.rushDist + 3 * (mineDensity * DataCache.rushDist)) + 50;
//					rc.setIndicatorString(1, "high cutoff: " + Integer.toString(shieldsCutoff));
					if (rc.getShields() > shieldsCutoff) {
						// leave
						//						if (!ourNukeHalfDone && enemyNukeHalfDone) {
						//							nextSoldierState = SoldierState.ALL_IN;
						//							allInCode();
						//						} else {
						nextSoldierState = SoldierState.RALLYING;
						rallyingCode();
						//						}
					}
				}
				//			}
			}
		}
	}
	
	public void expressChargeShieldsCode() throws GameActionException {		
		// find an empty space next to the shields encampment
		int distanceSquaredToShields = rc.getLocation().distanceSquaredTo(shieldLocation);
//		rc.setIndicatorString(2, "distance: " + distanceSquaredToShields);
		if (distanceSquaredToShields > 2) {
			// not charging yet
			for (int i = 8; --i >= 0; ) {
				// Check to see if it's empty
				MapLocation iterLocation = shieldLocation.add(DataCache.directionArray[i]);
				if (rc.senseObjectAtLocation(iterLocation) == null) {
					// Oh, there's an empty space! let's go to it
					NavSystem.goToLocation(iterLocation);
					return;
				}
			}
			// we found the shields encampment, but there are no empty spaces, so wait at the queue location
			//					NavSystem.goToLocation(EncampmentJobSystem.shieldsQueueLoc);
			NavSystem.goToLocation(shieldQueueLocation);
		} else {
			// already charging
			shieldsCutoff = DataCache.rushDist + 50;
//					rc.setIndicatorString(1, "high cutoff: " + Integer.toString(shieldsCutoff));
			if (rc.getShields() > shieldsCutoff) {
				nextSoldierState = SoldierState.ALL_IN;
				allInCode();
			}
		}
	}

	public void findingStartMinePositionsCode() throws GameActionException {
		if (DataCache.numEnemyRobots == 0) {
			int distanceSquaredToMiningStartLocation = rc.getLocation().distanceSquaredTo(miningStartLocation);
			if (distanceSquaredToMiningStartLocation == 0 ||
					(distanceSquaredToMiningStartLocation <= 2 && miningStartLocation.equals(DataCache.ourHQLocation))) {
				nextSoldierState = SoldierState.MINING;
				miningCode();
			} else if (distanceSquaredToMiningStartLocation <= 2 && rc.senseEncampmentSquares(miningStartLocation, 0, null).length == 1) {
				// Choose another miningStartLocation
				getNewMiningStartLocation();
			} else {
				Direction dir = rc.getLocation().directionTo(miningStartLocation);
				NavSystem.goDirectionAndDefuse(dir);
			}
		} else {
			nextSoldierState = SoldierState.FIGHTING;
			fightingCode();
		}
	}
	
	/**
	 * compares current health to health last turn and determines whether or not it got splashed damaged upon
	 * @param currHealth
	 * @param lastTurnHealth
	 * @return
	 */
	public boolean detectArtillerySplash(double currHealth, double lastTurnHealth) {
		int numEnemyNeighbors = rc.senseNearbyGameObjects(Robot.class, 2, rc.getTeam().opponent()).length;
		if (lastTurnHealth - currHealth > numEnemyNeighbors * 6 + 10) {
			System.out.println("artillery splash damage detected");
			return true;
		}
		return false;
	}

	/**
	 * Method that writes to a channel to inform others that an artillery has been seen.
	 * Primarily used by HQ to know to build a shields encampment.
	 * @throws GameActionException
	 */
	public boolean reportArtillerySighting(double currHealth) throws GameActionException {
		Robot[] nearbyEnemyRobots = rc.senseNearbyGameObjects(Robot.class, 14, rc.getTeam().opponent());
		for (Robot robot : nearbyEnemyRobots) {
			RobotInfo robotInfo = rc.senseRobotInfo(robot);
			if (robotInfo.type == RobotType.ARTILLERY) {
				BroadcastSystem.write(ChannelType.ARTILLERY_SEEN, Constants.TRUE);
				return true;
			}
		}
		
		if (detectArtillerySplash(currHealth, healthLastTurn)) {
			BroadcastSystem.write(ChannelType.ARTILLERY_SEEN, Constants.TRUE);
			return true;
		}
		return false;
	}
	
	private void defendMicro() throws GameActionException {
		Robot[] enemiesList = rc.senseNearbyGameObjects(Robot.class, 100000, rc.getTeam().opponent());
		int[] closestEnemyInfo = getClosestEnemy(enemiesList);
		MapLocation closestEnemyLocation = new MapLocation(closestEnemyInfo[1], closestEnemyInfo[2]);
		double[][] activities = enemyActivites (currentLocation, rc.getTeam());
		if (activities[2][0]+activities[1][0]+activities[0][0] == 0){
			NavSystem.goToLocationAvoidMines(closestEnemyLocation);
//			rc.setIndicatorString(0, "Forward1");
		}
		if (!minedUpAndReadyToGo(currentLocation)){
			if (activities[1][0]+activities[0][0]>2) {
//			if (activities[1][0]>1 || activities[0][0] == 0) {
				NavSystem.goToLocationAvoidMines(DataCache.ourHQLocation);
//				rc.setIndicatorString(0, "Back1");
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
//			rc.setIndicatorString(0, "Pos");
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
	
	public void miningSubroutine() throws GameActionException {
		if (DataCache.numEnemyRobots == 0 && rc.getLocation().distanceSquaredTo(DataCache.ourHQLocation) <= Constants.MAXIMUM_MINING_DISTANCE_SQUARED_FROM_HQ) {
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
						if (distanceToLine(newLocation1) <= Constants.MINING_WIDTH && rc.senseMine(newLocation1) == null && rc.senseEncampmentSquares(newLocation1, 0, null).length == 0) {
							NavSystem.goDirectionAndDefuse(miningDirConstant);
							return;
						} else if (distanceToLine(newLocation2) <= Constants.MINING_WIDTH && rc.senseMine(newLocation2) == null && rc.senseEncampmentSquares(newLocation2, 0, null).length == 0) {
							NavSystem.goDirectionAndDefuse(miningDirConstantOpp);
							return;
						}
						
						Direction dir = rc.getLocation().directionTo(miningDestination);
						NavSystem.goDirectionAndDefuse(dir);
					}
				}
			} else {
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
//						rc.setIndicatorString(0, Double.toString(distanceToLine(newLocation1)));
//						rc.setIndicatorString(1, Double.toString(distanceToLine(newLocation2)));
						if (distanceToLine(newLocation1) <= Constants.MINING_WIDTH && rc.senseMine(newLocation1) == null && rc.senseEncampmentSquares(newLocation1, 0, null).length == 0) {
							NavSystem.goDirectionAndDefuse(miningDirConstant);
							return;
						} else if (distanceToLine(newLocation2) <= Constants.MINING_WIDTH && rc.senseMine(newLocation2) == null && rc.senseEncampmentSquares(newLocation2, 0, null).length == 0) {
							NavSystem.goDirectionAndDefuse(miningDirConstantOpp);
							return;
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
			double[] our23 = getEnemies2Or3StepsAway();
			if (our23[0] < 1) { //if currently attacking encampments or idle workers
				if (our23[1] >= 1) {
					NavSystem.goAwayFromLocationAvoidMines(closestEnemyLocation);
				}
			}
		} else if (DataCache.numNearbyEnemySoldiers == 0 || DataCache.numNearbyAlliedSoldiers >= 3 * DataCache.numNearbyEnemySoldiers){ // if no enemies in one, two, or three dist
//			// if no enemies in 3-dist or we outnumber them 3 to 1
//			NavSystem.goToLocation(closestEnemyLocation);
			if (rc.isActive()) {
				NavSystem.moveCloserFavorNoMines(closestEnemyLocation);
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
					if (enemy23[1] + enemy23[0] > our23[1] + our23[2]+1 || our23[1] + our23[2] < 1) {
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
						NavSystem.goAwayFromLocationAvoidMines(closestEnemyLocation);
					}
				} else {
					if (enemy23[2] - our23[2] >= 6 || our23[2] < 1) {
						NavSystem.goToLocationAvoidMines(closestEnemyLocation);
					} else {
						NavSystem.goAwayFromLocationAvoidMines(closestEnemyLocation);
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
			if (rinfo.type == RobotType.SOLDIER && rinfo.roundsUntilMovementIdle < 3) {
				if (dist <= 2) {
					count1++;
				} else if (dist <=8) {
					count2++;
				} else if (dist > 8 && (dist <= 14 || dist == 18)) {
					count3++;
				}
			} else {
				if (dist <= 2) {
					count1 += 0.2;
				} else if (dist <=8) {
					count2 += 0.2;
				} else if (dist > 8 && (dist <= 14 || dist == 18)) {
					count3 += 0.2;
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
			if (rinfo.type == RobotType.SOLDIER && rinfo.roundsUntilMovementIdle < 3) {
				if (dist <= 2) {
					count1++;
				} else if (dist <=8) {
					count2++;
				} else if (dist <= 14 || dist == 18) {
					count3++;
				}
			} else {
				if (dist <= 2) {
					count1 += 0.2;
				} else if (dist <=8) {
					count2 += 0.2;
				} else if (dist <= 14 || dist == 18) {
					count3 += 0.2;
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
		
		if (DataCache.numNearbyAlliedSoldiers > 1.5 * DataCache.numNearbyEnemySoldiers) {
//			NavSystem.goToLocation(closestEnemyLocation);
			pushCodeGetCloser(closestEnemyLocation);
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
		pushCodeGetCloser(DataCache.enemyHQLocation);
	}
	
	public void pushCodeGetCloser(MapLocation destination) throws GameActionException {
		if (NavSystem.navMode != NavMode.GETCLOSER || NavSystem.destination != destination) {
			NavSystem.setupGetCloser(destination);
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
					NavSystem.moveCloserFavorNoMines(EncampmentJobSystem.goalLoc);
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
	
	private double findMineDensity() {
		int ourX = DataCache.ourHQLocation.x;
		int ourY = DataCache.ourHQLocation.y;
		int enemyX = DataCache.enemyHQLocation.x;
		int enemyY = DataCache.enemyHQLocation.y;
		
		MapLocation pt1 = new MapLocation(ourX + (enemyX-ourX)/8, ourY + (enemyY-ourY)/8);
		MapLocation pt2 = new MapLocation(ourX + 3*(enemyX-ourX)/8, ourY + 3*(enemyY-ourY)/8);
		MapLocation pt3 = new MapLocation(ourX + 5*(enemyX-ourX)/8, ourY + 5*(enemyY-ourY)/8);
		MapLocation pt4 = new MapLocation(ourX + 7*(enemyX-ourX)/8, ourY + 7*(enemyY-ourY)/8);

		int rSquared = DataCache.rushDistSquared / 64;
		double mineDensity1 = rc.senseMineLocations(pt1, rSquared, Team.NEUTRAL).length / (3.0 * rSquared);
		double mineDensity2 = rc.senseMineLocations(pt2, rSquared, Team.NEUTRAL).length / (3.0 * rSquared);
		double mineDensity3 = rc.senseMineLocations(pt3, rSquared, Team.NEUTRAL).length / (3.0 * rSquared);
		double mineDensity4 = rc.senseMineLocations(pt4, rSquared, Team.NEUTRAL).length / (3.0 * rSquared);

		return (mineDensity1 + mineDensity2 + mineDensity3 + mineDensity4)/4.0;

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
