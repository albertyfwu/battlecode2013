package base;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

public class HQRobot extends BaseRobot {

//	// these two arrays must have bijective correspondence
//	// the job associated with encampmentJobs[i] must always write to channelList[i] for each i
//	public MapLocation[] encampmentJobs = new MapLocation[EncampmentJobSystem.maxEncampmentJobs]; // length is constant
//	public ChannelType[] encampmentChannels = new ChannelType[EncampmentJobSystem.maxEncampmentJobs]; // length is constant
//
//	public int numEncampmentsNeeded; // must be less than encampmentJobChannelList.length
	public MapLocation HQLocation;
	public MapLocation EnemyHQLocation;

	public HQRobot(RobotController rc) {
		super(rc);
		HQLocation = rc.getLocation();
		EnemyHQLocation = rc.senseEnemyHQLocation();
		EncampmentJobSystem.initializeConstants(HQLocation);

	}

	@Override
	public void run() {
		try {
//			BroadcastSystem.write(ChannelType.CHANNEL1, 3493);
			
			
//			updateJobsAfterChecking();
//			System.out.println("unreachables: " + EncampmentJobSystem.numUnreachableEncampments);
//			EncampmentJobSystem.updateJobsAfterChecking();
			
			
			if (rc.isActive()) {
//				if (Clock.getRoundNum() > 2250) {
//					rc.resign();
//				}
				//				if (message.isValid) {
				//					if (message.body == 0xFFFFFF) {
				//						System.out.println("HQ 0xFFFFFF");
				//					} else {
				//						int locY = message.body & 0xFF;
				//						int locX = message.body >> 8;
				//						System.out.println("HQ locy: " + locY + " locx: " + locX);
				//					}
				//				}
				

				//				if (Clock.getRoundNum() % 10 == 0) {
				//					updateJobs();
				//				} else {
				//				}

//				// Spawn a soldier
//				Direction desiredDir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
//				Direction dir = getSpawnDirection(rc, desiredDir);
//				if (dir != null) {
//					EncampmentJobSystem.updateJobs();
//					rc.spawn(dir);
//				}
				if (Clock.getRoundNum() < 5) {
					Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
					rc.spawn(dir);
				}
			}
			
			for (int i=0; i<3; i++) {
				MapLocation job = EncampmentJobSystem.encampmentJobs[i];
				rc.setIndicatorString(i, "jobx: " + job.x + "joby: " + job.y);

			}

			//			// TODO: find out what strategy to switch to?
			//			// this.strategy = HQStrategy.xxx
			//			if (rc.isActive()) {
			//				switch (strategy) {
			//				case CREATE_SOLDIER:
			//					create_soldier();
			//					break;
			//				case RESEARCH_DEFUSION:
			//					if (!rc.hasUpgrade(Upgrade.DEFUSION)) {
			//						rc.researchUpgrade(Upgrade.DEFUSION);
			//					}
			//					break;
			//				case RESEARCH_FUSION:
			//					if (!rc.hasUpgrade(Upgrade.FUSION)) {
			//						rc.researchUpgrade(Upgrade.FUSION);
			//					}
			//					break;
			//				case RESEARCH_NUKE:
			//					if (!rc.hasUpgrade(Upgrade.NUKE)) {
			//						rc.researchUpgrade(Upgrade.NUKE);
			//					}
			//					break;
			//				case RESEARCH_PICKAXE:
			//					if (!rc.hasUpgrade(Upgrade.PICKAXE)) {
			//						rc.researchUpgrade(Upgrade.PICKAXE);
			//					}
			//					break;
			//				case RESEARCH_VISION:
			//					if (!rc.hasUpgrade(Upgrade.VISION)) {
			//						rc.researchUpgrade(Upgrade.VISION);
			//					}
			//					break;
			//				default:
			//					break;
			//				}
			//			}
		} catch (Exception e) {
			System.out.println("caught exception before it killed us:");
			System.out.println(rc.getRobot().getID());
			e.printStackTrace();
		}
	}

	public void create_soldier() throws GameActionException {
		// Spawn a soldier
		Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
		if (rc.canMove(dir)) {
			rc.spawn(dir);
		}
	}


	/**
	 * helper fcn to see what direction to actually go given a desired direction
	 * @param rc
	 * @param dir
	 * @return
	 */
	private static Direction getSpawnDirection(RobotController rc, Direction dir) {
		if (rc.canMove(dir)) {
			return dir;
		} else if (rc.canMove(dir.rotateLeft())) {
			return dir.rotateLeft();
		} else if (rc.canMove(dir.rotateRight())) {
			return dir.rotateRight();
		} else if (rc.canMove(dir.rotateLeft().rotateLeft())) {
			return dir.rotateLeft().rotateLeft();
		} else if (rc.canMove(dir.rotateRight().rotateRight())) {
			return dir.rotateRight().rotateRight();
		} else if (rc.canMove(dir.rotateLeft().opposite())) {
			return dir.rotateLeft().opposite();
		} else if (rc.canMove(dir.rotateRight().opposite())) {
			return dir.rotateRight().opposite();
		} else if (rc.canMove(dir.opposite())) {
			return dir.opposite();
		} else {
			return null;
		}
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
