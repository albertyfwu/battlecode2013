package base;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

public class HQRobot extends BaseRobot {

	// these two arrays must have bijective correspondence
	// the job associated with encampmentJobs[i] must always write to channelList[i] for each i
	public MapLocation[] encampmentJobs = new MapLocation[EncampmentJobSystem.maxEncampmentJobs]; // length is constant
	public ChannelType[] encampmentChannels = new ChannelType[EncampmentJobSystem.maxEncampmentJobs]; // length is constant

	public int numEncampmentsNeeded; // must be less than encampmentJobChannelList.length
	public MapLocation HQLocation;
	public MapLocation EnemyHQLocation;

	public HQRobot(RobotController rc) {
		super(rc);
		HQLocation = rc.getLocation();
		EnemyHQLocation = rc.senseEnemyHQLocation();
		numEncampmentsNeeded = Constants.INITIAL_NUM_ENCAMPMENTS_NEEDED; 

		MapLocation[] allEncampments = rc.senseAllEncampmentSquares();
		if (allEncampments.length < numEncampmentsNeeded) {
			numEncampmentsNeeded = allEncampments.length;
		}

		MapLocation[] closestEncampments = getClosestMapLocations(HQLocation, allEncampments, numEncampmentsNeeded);

		for (int i=0; i<numEncampmentsNeeded; i++) {
			// save in list of jobs
			encampmentJobs[i] = closestEncampments[i];

			// broadcast job opening
			encampmentChannels[i] = EncampmentJobSystem.encampmentJobChannelList[i];
			EncampmentJobSystem.postJob(encampmentChannels[i], encampmentJobs[i]);
		}

	}

	@Override
	public void run() {
		try {
			
			updateJobsAfterChecking();
			
			
			if (rc.isActive()) {
//				if (Clock.getRoundNum() > 50) {
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

				// Spawn a soldier
				Direction desiredDir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
				Direction dir = getSpawnDirection(rc, desiredDir);
				if (dir != null) {
					updateJobs();
					rc.spawn(dir);
				}

			}
			
			for (int i=0; i<3; i++) {
				MapLocation job = encampmentJobs[i];
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
	 * checks nearby encampments to see if jobs need to be changed
	 * @throws GameActionException
	 */
	public void updateJobs() throws GameActionException {
		System.out.println("Before update: " + Clock.getBytecodeNum());
		MapLocation[] neutralEncampments = rc.senseEncampmentSquares(HQLocation,100000, Team.NEUTRAL);
		if (numEncampmentsNeeded > neutralEncampments.length){
			numEncampmentsNeeded = neutralEncampments.length;
		}

		MapLocation[] newJobsList = getClosestMapLocations(HQLocation, neutralEncampments, numEncampmentsNeeded);
		System.out.println("new jobs list: " + Clock.getBytecodeNum());

		ChannelType[] channelList = assignChannels(newJobsList, encampmentJobs, encampmentChannels);

		for (int i=0; i<numEncampmentsNeeded; i++) { // update lists
			encampmentJobs[i] = newJobsList[i];
			encampmentChannels[i] = channelList[i];
		}			

		System.out.println("update lists: " + Clock.getBytecodeNum());

		// clear unused channels
		for (ChannelType channel: EncampmentJobSystem.encampmentJobChannelList) {
			if (arrayIndex(channel, encampmentChannels) == -1) { // if unused
				BroadcastSystem.writeMaxMessage(channel); // reset the channel
			}
		}
		
		
		System.out.println("After update: " + Clock.getRoundNum());
	}
	/**
	 * HQ uses this to check if any jobs are completed, and then updates new jobs
	 * @throws GameActionException
	 */
	public void updateJobsAfterChecking() throws GameActionException {
		MapLocation[] completedJobs = EncampmentJobSystem.checkAllCompletion();
		int numNonNull = completedJobs[completedJobs.length-1].x;
		if (numNonNull > 0) { // if it contains non-null elements
			updateJobs();
		}

	}

	public static ChannelType[] assignChannels(MapLocation[] newJobsList, MapLocation[] oldJobsList, ChannelType[] oldChannelsList) {
		ChannelType[] channelList = new ChannelType[EncampmentJobSystem.maxEncampmentJobs];

		int arrayIndices[] = new int[newJobsList.length];

		for (int i=0; i<newJobsList.length; i++) {
			arrayIndices[i] = arrayIndex(newJobsList[i], oldJobsList);
		}


		int channelIndex = -1;
		// keep old channels for non-new jobs
		for (int i=0; i<newJobsList.length; i++) {
			if (arrayIndices[i] != -1 && newJobsList[i] != null) { // if the job is not new, keep the same channel
				channelList[i] = oldChannelsList[arrayIndices[i]];
			}
		}

		// allocate unused channels for new jobs
		for (int i=0; i<newJobsList.length; i++) {
			if (arrayIndices[i] == -1 && newJobsList[i] != null) {
				channelLoop: for (ChannelType channel: EncampmentJobSystem.encampmentJobChannelList) {
					channelIndex = arrayIndex(channel, channelList);
					if (channelIndex == -1) { // if not already used, use it and post job
						channelList[i] = channel;
						EncampmentJobSystem.postJob(channel, newJobsList[i]);
						break channelLoop;
					}
				}
			}
		}
		return channelList;
	}

	/** 
	 * returns index of element e in array
	 * @param e
	 * @param array
	 * @return
	 */
	public static int arrayIndex(Object e, Object[] array) {
		if (e == null) {
			return -1;
		}

		for (int i = 0; i<array.length; i++) {
			if (array[i] != null) {
				if (array[i].equals(e)) {
					return i;
				}
			}
		}
		return -1;
	}

	/**
	 * Finds array of closest MapLocations to rc.getLocation()
	 * in decreasing order
	 * 
	 * Specification: k must be <= allLoc.length
	 * Specification: array must not contain origin
	 * @param origin
	 * @param allLoc
	 * @param k
	 * @return
	 * 
	 * This still costs lots of bytecodes
	 */
	public static MapLocation[] getClosestMapLocations(MapLocation origin, MapLocation[] allLoc, int k) {
		MapLocation[] currentTopLocations = new MapLocation[k];

		int[] allDistances = new int[allLoc.length];
		for (int i=0; i<allLoc.length; i++) {
			allDistances[i] = origin.distanceSquaredTo(allLoc[i]);
		}

		int[] allLocIndex = new int[allLoc.length];

		int runningDist = 1000000;
		MapLocation runningLoc = null;
		int runningIndex = 0;
		for (int j = 0; j < k; j++) {
			runningDist = 1000000;
			for (int i=0; i<allLoc.length; i++) {
				if (allDistances[i] < runningDist && allLocIndex[i] == 0) {
					runningDist = allDistances[i];
					runningLoc = allLoc[i];
					runningIndex = i;
				}
			}
			currentTopLocations[j] = runningLoc;
			allLocIndex[runningIndex] = 1;
		}

		return currentTopLocations;
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
		} else {
			return dir.opposite();
		}
	}

	public static void main(String[] arg0) {
		MapLocation origin = new MapLocation(0,0);

		MapLocation test1 = new MapLocation(5,0);
		MapLocation test2 = new MapLocation(4,1);
		MapLocation test3 = new MapLocation(3,2);
		MapLocation test4 = new MapLocation(1,3);
		MapLocation test5 = new MapLocation(6,2);
		MapLocation test6 = new MapLocation(0,5);
		MapLocation test7 = new MapLocation(2,3);

		MapLocation[] oldJobsList = {test1, test2, test3, test4, null};
		MapLocation[] newJobsList = {test2, test4, test5, null, null};
		ChannelType[] oldChannelsList = {ChannelType.ENC1, ChannelType.ENC2, ChannelType.ENC3, ChannelType.ENC4, ChannelType.ENC5};

		ChannelType[] newChannelsList = assignChannels(newJobsList, oldJobsList, oldChannelsList);

		for (int i=0; i<newChannelsList.length; i++) {
			System.out.println("result " + i + ": " + newChannelsList[i]);
		}

	}
}
