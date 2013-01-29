package team162;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;
import battlecode.common.Upgrade;

public class HQRobot extends BaseRobot {
	
	public int move_out_round;
	
	public ChannelType powerChannel = ChannelType.HQPOWERLEVEL;
	public ChannelType strategyChannel = ChannelType.STRATEGY;
	public ChannelType genCountChannel = ChannelType.GEN_COUNT;
	
	public boolean artillerySeen = false;
	
	public boolean allInMode = false;
	public boolean ourNukeHalfDone = false;

	public HQRobot(RobotController rc) throws GameActionException {
		super(rc);
		decideStrategy();
		
//		if (rc.getTeam() == Team.A) {
//			strategy = Strategy.ECON;
//		} else {
//			strategy = Strategy.NUKE;
//		}
		
		EncampmentJobSystem.initializeConstants();
	}
	
	/**
	 * Sets the name strategy (along with other variables) that is best suited for the given map.
	 * 
	 * Important considerations:
	 * 1. rush distance
	 * 2. mine density
	 * 3. how many encampment squares are close to the base, and in what formation are they?
	 * 
	 * @throws GameActionException
	 */
	public void decideStrategy() throws GameActionException {
		// possible locations for building artillery if we're going to do nuke strategy
		MapLocation[] possibleArtilleryLocations = EncampmentJobSystem.getPossibleArtilleryLocations();
		int numPossibleArtilleryLocations = possibleArtilleryLocations.length;
		// How close are they?
		int averageDistanceSquared = 0;
		for (MapLocation location : possibleArtilleryLocations) {
			averageDistanceSquared += location.distanceSquaredTo(DataCache.ourHQLocation);
		}
		if (numPossibleArtilleryLocations == 0) {
			averageDistanceSquared = Integer.MAX_VALUE;
		} else {
			averageDistanceSquared /= numPossibleArtilleryLocations;
		}
		
		// mine density - is this the best measure? what about getting the line
		// between the two HQs and counting mines that lie within a certain constant
		// of the line? i guess the second option would be incredibly expensive...
		// maybe we could just query senseMineLocations two or four times? to approximate a line?
		MapLocation midPoint = findMidPoint();
		int rSquared = DataCache.rushDistSquared / 4;
		double mineDensity = rc.senseMineLocations(midPoint, rSquared, Team.NEUTRAL).length / (3.0 * rSquared);
		
		strategy = Strategy.ECON;
	}
	
	private MapLocation findMidPoint() {
		MapLocation enemyLoc = DataCache.enemyHQLocation;
		MapLocation ourLoc = DataCache.ourHQLocation;
		int x, y;
		x = (enemyLoc.x + ourLoc.x) / 2;
		y = (enemyLoc.y + ourLoc.y) / 2;
		return new MapLocation(x,y);
	}
	
	@Override
	public void run() {
		try {
			
			rc.setIndicatorString(2, Integer.toString(move_out_round));
//			rc.setIndicatorString(0, Integer.toString(rc.checkResearchProgress(Upgrade.NUKE)));
//			if (Clock.getRoundNum() > 50) {
//				BroadcastSystem.write(ChannelType.ARTILLERY_SEEN, Constants.TRUE);
//			}

			DataCache.updateRoundVariables();
			BroadcastSystem.write(powerChannel, (int) rc.getTeamPower()); // broadcast the team power
			BroadcastSystem.write(strategyChannel, strategy.ordinal()); // broadcast the strategy
			BroadcastSystem.write(genCountChannel, EncampmentJobSystem.genCount); // broadcast the number of generators we've built\
			
			if (Clock.getRoundNum() % Constants.CHANNEL_CYCLE == 0 && Clock.getRoundNum() > 0) {
				persistRetreatChannel();
			}
			
			// Check if our nuke is half done
			if (!ourNukeHalfDone) {
				if (rc.checkResearchProgress(Upgrade.NUKE) >= 200) {
					ourNukeHalfDone = true;
				}
			}
			if (ourNukeHalfDone) {
				// Broadcast this
				BroadcastSystem.write(ChannelType.OUR_NUKE_HALF_DONE, 1);
			}
			// Check if enemy's nuke is half done
			if (!enemyNukeHalfDone) {
				enemyNukeHalfDone = rc.senseEnemyNukeHalfDone();
				if (enemyNukeHalfDone) {
					move_out_round = Clock.getRoundNum() + 140 - DataCache.rushDist;
				}
			}
			if (enemyNukeHalfDone) {
				// If it is half done, broadcast it
				if (!ourNukeHalfDone) {
					allInMode = true;
				}
				BroadcastSystem.write(ChannelType.ENEMY_NUKE_HALF_DONE, 1);				
			}			
			
			if (move_out_round > 0) {
				BroadcastSystem.write(ChannelType.MOVE_OUT, move_out_round);
			}
			
			if (artillerySeen == false) {
				Message message;
				if (Clock.getRoundNum() % Constants.CHANNEL_CYCLE == 0 && Clock.getRoundNum() > 0) {
					message = BroadcastSystem.readLastCycle(ChannelType.ARTILLERY_SEEN);
				} else {
					message = BroadcastSystem.read(ChannelType.ARTILLERY_SEEN);
				}
				if ((message.isValid && message.body == Constants.TRUE) || enemyNukeHalfDone) {
					artillerySeen = true;
					BroadcastSystem.write(ChannelType.ARTILLERY_SEEN, Constants.TRUE); // write artillery seen
					EncampmentJobSystem.setShieldLocation();
					if (EncampmentJobSystem.shieldsLoc != null) {
						EncampmentJobSystem.postShieldLocation();
					}
					System.out.println("artilleryseen");
				}
			} else if (EncampmentJobSystem.shieldsLoc != null) {
				if (!rc.canSenseSquare(EncampmentJobSystem.shieldsLoc) || !(rc.senseEncampmentSquares(EncampmentJobSystem.shieldsLoc, 0, rc.getTeam()).length > 0)) {
					// post shield encampment job
					EncampmentJobSystem.updateShieldJob();
				}
				if (Clock.getRoundNum() % Constants.CHANNEL_CYCLE == 0 && Clock.getRoundNum() > 0) {
					EncampmentJobSystem.checkShieldCompletionOnCycle();
					EncampmentJobSystem.postShieldLocation(); // to make sure the the shield location isn't lost
				} else {
					EncampmentJobSystem.checkShieldCompletion();
				}
			}
			
			// to handle broadcasting between channel cycles
			if (Clock.getRoundNum() % Constants.CHANNEL_CYCLE == 0 && Clock.getRoundNum() > 0) {
                EncampmentJobSystem.updateJobsOnCycle();
	        } else {
	            EncampmentJobSystem.updateJobsAfterChecking();
	        }
			
			rc.setIndicatorString(0, Double.toString((40 + 10 * (EncampmentJobSystem.genCount))/1.5));
			rc.setIndicatorString(1, Integer.toString(DataCache.numAlliedSoldiers));
			
			if (rc.isActive()) {
				if (strategy == Strategy.ECON || strategy == Strategy.RUSH) {
					boolean upgrade = false;
					if (enemyNukeHalfDone && !DataCache.hasDefusion && DataCache.numAlliedSoldiers > 5) {
						if (!DataCache.hasDefusion) {
							upgrade = true;
							rc.researchUpgrade(Upgrade.DEFUSION);
						}
					} else if (DataCache.numAlliedRobots >= (40 + 10 * (EncampmentJobSystem.genCount))/1.5 ) {
						if (!DataCache.hasDefusion) {
							upgrade = true;
							rc.researchUpgrade(Upgrade.DEFUSION);
						} else if (!DataCache.hasFusion) {
							upgrade = true;
							rc.researchUpgrade(Upgrade.FUSION);
						} else if (!DataCache.hasPickaxe) {
							upgrade = true;
							rc.researchUpgrade(Upgrade.PICKAXE);

						}
					}
					if (!upgrade) {
						spawnSoldier();
					}
				} else if (strategy == Strategy.NUKE) {
					boolean upgrade = false;
					if (allInMode) {
						if (!DataCache.hasDefusion) {
							upgrade = true;
							rc.researchUpgrade(Upgrade.DEFUSION);
						}						
						if (!upgrade) {
							spawnSoldier();
						}
					} else {
//						if ((DataCache.numAlliedRobots >= 9 && Clock.getRoundNum() > 5) ||
						if ((rc.getTeamPower() < 150 && Clock.getRoundNum() > 5) ||
								(DataCache.numNearbyEnemySoldiers == 0 && rc.checkResearchProgress(Upgrade.NUKE) > 385 && rc.getEnergon() > 475)) {
							upgrade = true;
							rc.researchUpgrade(Upgrade.NUKE);
						}
						if (!upgrade) {
							spawnSoldier();
						}
					}
				}
			}
		} catch (Exception e) {
			System.out.println("caught exception before it killed us:");
			System.out.println(rc.getRobot().getID());
			e.printStackTrace();
		}
	}
	
	public void persistRetreatChannel() {
		Message msg = BroadcastSystem.readLastCycle(ChannelType.RETREAT_CHANNEL);
		if (msg.isValid && msg.body == Constants.RETREAT) { // if needs to be persisted
			BroadcastSystem.write(ChannelType.RETREAT_CHANNEL, Constants.RETREAT);
		}
	}
	
	public void spawnSoldier() throws GameActionException {
		Direction desiredDir = rc.getLocation().directionTo(DataCache.enemyHQLocation);
		Direction dir = getSpawnDirection(desiredDir);
		if (dir != null) {
			rc.spawn(dir);
		}
	}

	/**
	 * helper fcn to see what direction to actually go given a desired direction
	 * @param rc
	 * @param dir
	 * @return
	 */
	private Direction getSpawnDirection(Direction dir) {
		Direction canMoveDirection = null;
		int desiredDirOffset = dir.ordinal();
		int[] dirOffsets = new int[]{4, -3, 3, -2, 2, -1, 1, 0};
		for (int i = dirOffsets.length; --i >= 0; ) {
			int dirOffset = dirOffsets[i];
			Direction currentDirection = DataCache.directionArray[(desiredDirOffset + dirOffset + 8) % 8];
			if (rc.canMove(currentDirection)) {
				if (canMoveDirection == null) {
					canMoveDirection = currentDirection;
				}
				Team mineTeam = rc.senseMine(rc.getLocation().add(currentDirection));
				if (mineTeam == null || mineTeam == rc.getTeam()) {
					// If there's no mine here or the mine is an allied mine, we can spawn here
					return currentDirection;
				}
			}			
		}
		// Otherwise, let's just spawn in the desired direction, and make sure to clear out a path later
		return canMoveDirection;
	}

//	public static void main(String[] arg0) {
//		MapLocation origin = new MapLocation(0,0);
//
//		MapLocation test1 = new MapLocation(5,0);
//		MapLocation test2 = new MapLocation(4,1);
//		MapLocation test3 = new MapLocation(3,2);
//		MapLocation test4 = new MapLocation(1,3);
//		MapLocation test5 = new MapLocation(6,2);
//		MapLocation test6 = new MapLocation(0,5);
//		MapLocation test7 = new MapLocation(2,3);
//
//		MapLocation[] oldJobsList = {test1, test2, test3, test4, null};
//		MapLocation[] newJobsList = {test2, test4, test5, null, null};
//		ChannelType[] oldChannelsList = {ChannelType.ENC1, ChannelType.ENC2, ChannelType.ENC3, ChannelType.ENC4, ChannelType.ENC5};
//
//		ChannelType[] newChannelsList = EncampmentJobSystem.assignChannels(newJobsList, oldJobsList, oldChannelsList);
//
//		for (int i=0; i<newChannelsList.length; i++) {
//			System.out.println("result " + i + ": " + newChannelsList[i]);
//		}
//
//	}
}
