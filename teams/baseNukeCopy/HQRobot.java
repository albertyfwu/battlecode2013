package baseNukeCopy;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;
import battlecode.common.Upgrade;

public class HQRobot extends BaseRobot {

//	// these two arrays must have bijective correspondence
//	// the job associated with encampmentJobs[i] must always write to channelList[i] for each i
//	public MapLocation[] encampmentJobs = new MapLocation[EncampmentJobSystem.maxEncampmentJobs]; // length is constant
//	public ChannelType[] encampmentChannels = new ChannelType[EncampmentJobSystem.maxEncampmentJobs]; // length is constant
//
//	public int numEncampmentsNeeded; // must be less than encampmentJobChannelList.length
	public MapLocation HQLocation;
	public MapLocation EnemyHQLocation;
	public ChannelType powerChannel = ChannelType.HQPOWERLEVEL;
	
	public int enemyNukeHalfDoneFirstRound; // the first round that we sense the enemy nuke is half done

	public HQRobot(RobotController rc) throws GameActionException {
		super(rc);
		HQLocation = rc.getLocation();
		EnemyHQLocation = rc.senseEnemyHQLocation();
		EncampmentJobSystem.initializeConstants(HQLocation, EnemyHQLocation);

	}
	
	@Override
	public void run() {
		try {
			BroadcastSystem.write(powerChannel, (int) rc.getTeamPower());
			DataCache.updateRoundVariables();
			
			// Check if enemy's nuke is half done
			if (!enemyNukeHalfDone) {
				enemyNukeHalfDone = rc.senseEnemyNukeHalfDone();
				enemyNukeHalfDoneFirstRound = Clock.getRoundNum();
			}
			if (enemyNukeHalfDone) {
				// Broadcast this
				BroadcastSystem.write(ChannelType.ENEMY_NUKE_HALF_DONE, 1);
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
			
			if (Clock.getRoundNum() % Constants.CHANNEL_CYCLE == 0 && Clock.getRoundNum() > 0) {
                EncampmentJobSystem.updateJobsOnCycle();
	        } else {
	            EncampmentJobSystem.updateJobsAfterChecking();
	        }
			
			if (rc.isActive()) {
				boolean upgrade = false;
				if (rc.getTeamPower() < 150) {
					upgrade = true;
					rc.researchUpgrade(Upgrade.NUKE);
				}
				if (!upgrade) {
					// Spawn a soldier
					Direction desiredDir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
					Direction dir = getSpawnDirection(rc, desiredDir);
					if (dir != null && rc.isActive()) {
						EncampmentJobSystem.updateJobs();
						rc.spawn(dir);
					}
				}
			}
		} catch (Exception e) {
			System.out.println("caught exception before it killed us:");
			System.out.println(rc.getRobot().getID());
			e.printStackTrace();
		}
	}

	/**
	 * helper fcn to see what direction to actually go given a desired direction
	 * @param rc
	 * @param dir
	 * @return
	 */
	private static Direction getSpawnDirection(RobotController rc, Direction dir) {
		Direction canMoveDirection = null;
		int desiredDirOffset = dir.ordinal();
		int[] dirOffsets = new int[]{0, 1, -1, 2, -2, 3, -3, 4};
		for (int dirOffset : dirOffsets) {
			Direction currentDirection = Direction.values()[(desiredDirOffset + dirOffset + 8) % 8];
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
