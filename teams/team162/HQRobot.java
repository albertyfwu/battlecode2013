package team162;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.Team;
import battlecode.common.Upgrade;

public class HQRobot extends BaseRobot {
	
	public int move_out_round;
	
	public ChannelType powerChannel = ChannelType.HQPOWERLEVEL;
	public ChannelType genCountChannel = ChannelType.GEN_COUNT;
	
	public boolean artillerySeen = false;

	public HQRobot(RobotController rc) throws GameActionException {
		super(rc);
		
		EncampmentJobSystem.initializeConstants();
	}
	
	@Override
	public void run() {
		try {
			DataCache.updateRoundVariables();
			BroadcastSystem.write(powerChannel, (int) rc.getTeamPower()); // broadcast the team power
			BroadcastSystem.write(genCountChannel, EncampmentJobSystem.genCount); // broadcast the number of generators we've built\
			
			// Do a quicksort for the EncampmentJobSystem so it saves bytecode later
			if (Clock.getRoundNum() == 6) {
				EncampmentJobSystem.sortNeutralEncampmentScores();
			}
			// end quicksort test
			
			if (DataCache.onCycle) {
				persistRetreatChannel();
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
				BroadcastSystem.write(ChannelType.ENEMY_NUKE_HALF_DONE, Constants.TRUE);				
			}			
			
			if (move_out_round > 0) {
				BroadcastSystem.write(ChannelType.MOVE_OUT, move_out_round);
			}
			
			if (!artillerySeen) {
				Message message;
				if (DataCache.onCycle) {
					message = BroadcastSystem.readLastCycle(ChannelType.ARTILLERY_SEEN);
				} else {
					message = BroadcastSystem.read(ChannelType.ARTILLERY_SEEN);
				}
				if ((message.isValid && message.body == Constants.TRUE) || enemyNukeHalfDone || Clock.getRoundNum() >= 200) {
					artillerySeen = true;
					BroadcastSystem.write(ChannelType.ARTILLERY_SEEN, Constants.TRUE); // write artillery seen
					EncampmentJobSystem.setShieldLocation();
					if (EncampmentJobSystem.shieldsLoc != null) {
						EncampmentJobSystem.postShieldLocation();
					}
				}
			} else if (EncampmentJobSystem.shieldsLoc != null) {
				if (!rc.canSenseSquare(EncampmentJobSystem.shieldsLoc) || rc.senseEncampmentSquares(EncampmentJobSystem.shieldsLoc, 0, rc.getTeam()).length == 0) {
					// post shield encampment job
					EncampmentJobSystem.updateShieldJob();
				}
				if (DataCache.onCycle) {
					EncampmentJobSystem.checkShieldCompletionOnCycle();
					EncampmentJobSystem.postShieldLocation(); // to make sure the the shield location isn't lost
				} else {
					EncampmentJobSystem.checkShieldCompletion();
				}
			}

			// to handle broadcasting between channel cycles
			if (DataCache.onCycle) {
                EncampmentJobSystem.updateJobsOnCycle();
	        } else {
	            EncampmentJobSystem.updateJobsAfterChecking();
	        }
			
			if (rc.isActive()) {
				boolean upgrade = false;
				if (enemyNukeHalfDone && !DataCache.hasDefusion && DataCache.numAlliedSoldiers > 5) {
					if (!DataCache.hasDefusion) {
						upgrade = true;
						rc.researchUpgrade(Upgrade.DEFUSION);
					}
				} else if (rc.getTeamPower() <= 10 * (1 + DataCache.numAlliedEncampments)) {
					if (!DataCache.hasFusion) {
						upgrade = true;
						rc.researchUpgrade(Upgrade.FUSION);
					} else if (!DataCache.hasDefusion) {
						upgrade = true;
						rc.researchUpgrade(Upgrade.DEFUSION);
					} else if (!DataCache.hasVision) {
						upgrade = true;
						rc.researchUpgrade(Upgrade.VISION);
					} else {
						upgrade = true;
						rc.researchUpgrade(Upgrade.NUKE);
					}
				}
				if (!upgrade) {
					spawnSoldier();
				}
			}
		} catch (Exception e) {
//			System.out.println("caught exception before it killed us:");
//			System.out.println(rc.getRobot().getID());
//			e.printStackTrace();
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
	
//	public static void testing(int[] arr) {
//		arr[0] = 3;
//	}

//	public static void main(String[] arg0) {
//		int[] testArr = new int[] {1};
//		testing(testArr);
//		System.out.println(testArr[0]);
//	}
}
