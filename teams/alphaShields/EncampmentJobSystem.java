package alphaShields;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

/**
 * Job system for encampments
 */
public class EncampmentJobSystem {
	public static BaseRobot robot;
	public static RobotController rc;
	public static MapLocation goalLoc;
	
	public static MapLocation shieldsLoc; // location of the shields encampment
	public static ChannelType shieldChannel = ChannelType.ENCSHIELD;
	public static ChannelType shieldCompChannel = ChannelType.COMPSHIELD;
	
	public static ChannelType[] encampmentJobChannelList = 
		{ChannelType.ENC1,
		 ChannelType.ENC2,
		 ChannelType.ENC3,
		 ChannelType.ENC4,
		 ChannelType.ENC5}; // list of encampment channels

	public static ChannelType[] encampmentCompletionChannelList =
		{ChannelType.COMP1,
		 ChannelType.COMP2,
		 ChannelType.COMP3,
		 ChannelType.COMP4,
		 ChannelType.COMP5}; // list of encampment completion channels
	
	public static int maxEncampmentJobs = encampmentJobChannelList.length;
	public static int maxMessage = Constants.MAX_MESSAGE; //24-bit message all 1s	
	
	// these two arrays must have bijective correspondence
	// the job associated with encampmentJobs[i] must always write to channelList[i] for each i
	public static MapLocation[] encampmentJobs = new MapLocation[maxEncampmentJobs]; // length is constant
	public static ChannelType[] encampmentChannels = new ChannelType[maxEncampmentJobs]; // length is constant

	public static int numEncampmentsNeeded; // must be less than encampmentJobChannelList.length
	
	public static FastLocSet unreachableEncampments = new FastLocSet();;
	public static int numUnreachableEncampments;
	
	public static RobotType assignedRobotType;
	public static ChannelType assignedChannel;
	
	public static int supCount;
	public static int genCount;
	
	public static int artCount;
	
	public static int hardEncampmentLimit;
	
	public static boolean shieldJobOustanding = false;
	
	/**
	 * Initializes BroadcastSystem by setting rc
	 * @param myRobot
	 */
	public static void init(BaseRobot myRobot) {
		robot = myRobot;
		rc = robot.rc;
	}
	
	public static void initializeConstants() throws GameActionException {		
		numEncampmentsNeeded = Constants.INITIAL_NUM_ENCAMPMENTS_NEEDED; 
		numUnreachableEncampments = 0;
		supCount = 0;
		genCount = 0;
		artCount = 0;
		if (robot.strategy == Strategy.NUKE) {
			hardEncampmentLimit = 3;
		} else {
			hardEncampmentLimit = Integer.MAX_VALUE;
		}
		
		
		
		MapLocation[] allEncampments = rc.senseEncampmentSquares(DataCache.ourHQLocation, 10000, Team.NEUTRAL);
		
		MapLocation[] nearbyEncampments = rc.senseEncampmentSquares(DataCache.ourHQLocation, 9, Team.NEUTRAL);
		
		for (int i = nearbyEncampments.length; --i >= 0; ) {
			MapLocation encLoc = nearbyEncampments[i];
			if (encLoc.x == DataCache.ourHQLocation.x){
				unreachableEncampments.add(encLoc);
				numUnreachableEncampments++;
			}
		}
		
		
		if (robot.strategy == Strategy.NUKE){ // if nuke strat
			MapLocation[] possibleEncampments = getPossibleArtilleryLocations();
//			System.out.println("numPossibleEncampments: " + possibleEncampments.length);
			if (possibleEncampments.length == 0) {
				numEncampmentsNeeded = 0;
			} else {			
				if (possibleEncampments.length - numUnreachableEncampments > 0) {
					numEncampmentsNeeded = Math.min(3, possibleEncampments.length - numUnreachableEncampments);
					MapLocation[] closestEncampments = getClosestMapLocations(DataCache.ourHQLocation, possibleEncampments, numEncampmentsNeeded);

					for (int i=0; i<numEncampmentsNeeded; i++) {
						// save in list of jobs
						encampmentJobs[i] = closestEncampments[i];

						// broadcast job opening
						encampmentChannels[i] = encampmentJobChannelList[i];

						postJob(EncampmentJobSystem.encampmentChannels[i], encampmentJobs[i], getRobotTypeToBuild());
					}

				} else {
					numEncampmentsNeeded = 0;
				}	
			}
		} else { // if non-nuke match
			int numReachableEncampments = allEncampments.length - numUnreachableEncampments;
			if (numReachableEncampments == 0) {
				numEncampmentsNeeded = 0;
			} else if (numReachableEncampments < 10) {
				numEncampmentsNeeded = 1;
			} else if (numReachableEncampments < 30) {
				numEncampmentsNeeded = 2;
			} else {
				numEncampmentsNeeded = 3;
			}

			MapLocation[] closestEncampments = getClosestMapLocations(DataCache.ourHQLocation, allEncampments, numEncampmentsNeeded);

			for (int i = numEncampmentsNeeded; --i >= 0; ) {
				// save in list of jobs
				encampmentJobs[i] = closestEncampments[i];

				// broadcast job opening
				encampmentChannels[i] = encampmentJobChannelList[i];
				
				postJob(EncampmentJobSystem.encampmentChannels[i], encampmentJobs[i], getRobotTypeToBuild());
			}
		}
		
		
	}
	
	public static void updateShieldJob() {
		if (shieldJobOustanding == false) {
			postShieldJob();
		} 
	}
	
	/**
	 * HQ uses this to post a shield encampment job
	 * @param loc
	 */
	public static void postShieldJob() {
		postJobWithoutIncrementing(shieldChannel, shieldsLoc, 3);
		shieldJobOustanding = true;
//		System.out.println("shield job posted");
	}
	
	/**
	 * soldiers and encampments use this to read the channel
	 * @return
	 */
	public static MapLocation readShieldLocation() {
		Message msg = BroadcastSystem.read(ChannelType.SHIELD_LOCATION);
		if (msg.isValid && msg.body != Constants.MAX_MESSAGE) {
//			System.out.println("msgbody: " + msg.body);
			return parseShieldLocation(msg.body);
		}
		
		return null;
	}
	
	/**
	 * HQ uses this to post the shield location so encampments can suicide if necessary 
	 * and soldiers read this to find out where the shield is
	 */
	public static void postShieldLocation() {
		int msg = createShieldLocMessage(shieldsLoc);
//		System.out.println("shieldLoc:" + shieldsLoc);
		BroadcastSystem.write(ChannelType.SHIELD_LOCATION, msg);
	}
	/**
	 * HQ uses this to post a new job, and increments genCount and supCount
	 * @param channel
	 * @param loc
	 */
	public static void postJob(ChannelType channel, MapLocation loc, int robType) {
		BroadcastSystem.write(channel, createMessage(loc, false, true, robType));
		switch(robType) {
		case 1:
			genCount++;
			break;
		case 0:
			supCount++;
			break;
		case 2:
			artCount++;
			break;
		}
	}
	
	/**
	 * same as above, except without incrementing
	 * @param channel
	 * @param loc
	 */
	public static void postJobWithoutIncrementing(ChannelType channel, MapLocation loc, int robType) {
		BroadcastSystem.write(channel, createMessage(loc, false, true, robType));
	}
	
	/**
	 * Soldiers use this to update the job to claim that it's taken
	 * They also use this every turn to ping back to others
	 * @param channel
	 */
	public static void updateJobTaken() {
		int robotTypeInt = 0;
		switch(assignedRobotType) {
		case GENERATOR:
			robotTypeInt = 1;
			break;
		case SUPPLIER:
			robotTypeInt = 0;
			break;
		case ARTILLERY:
			robotTypeInt = 2;
			break;
		default:
			break;	
		}
		BroadcastSystem.write(assignedChannel, createMessage(goalLoc, true, true, robotTypeInt));
	}
	
	/**
	 * Soldiers use this to find an untaken job or a taken job that has been inactive
	 * @return ChannelType channel
	 * @throws GameActionException 
	 */
	public static ChannelType findJob() throws GameActionException {
		int currentRoundNum = Clock.getRoundNum();
		
		
		ChannelType channel = shieldChannel;
		Message message = BroadcastSystem.read(channel);
		int onOrOff;
		int isTaken;
		
		if (message.isValid && message.body != maxMessage) {
			
//			System.out.println("msgbody: " + message.body);
			onOrOff = parseOnOrOff(message.body);
			isTaken = parseTaken(message.body);
			if (onOrOff == 1 && isTaken == 0) { //if job is on and untaken
				goalLoc = parseLocation(message.body);
				if (rc.canSenseSquare(goalLoc)) {
					GameObject robotOnSquare = rc.senseObjectAtLocation(goalLoc);
					if (robotOnSquare == null || !robotOnSquare.getTeam().equals(rc.getTeam())) {
						assignedRobotType = parseRobotType(message.body);
						assignedChannel = channel;
//						rc.setIndicatorString(1, goalLoc.toString());
						return channel;
					}
				} else {
					assignedRobotType = parseRobotType(message.body);
					assignedChannel = channel;
//					rc.setIndicatorString(1, goalLoc.toString());
					return channel;
				}
			} else if (onOrOff == 1) {
				int postedRoundNum = parseRoundNum(message.body);
				if ((16+currentRoundNum - postedRoundNum)%16 >= 4) { // it hasn't been written on for 5 turns
					goalLoc = parseLocation(message.body);

//					System.out.println("posted: " + postedRoundNum);
//					System.out.println("current: " + currentRoundNum % 16);
//
					if (rc.canSenseSquare(goalLoc)) {
						GameObject robotOnSquare = rc.senseObjectAtLocation(goalLoc);
						if (robotOnSquare == null || !robotOnSquare.getTeam().equals(rc.getTeam())) {
							assignedRobotType = parseRobotType(message.body);
							assignedChannel = channel;
//							rc.setIndicatorString(1, goalLoc.toString());
							return channel;
						}
					} else {
						assignedRobotType = parseRobotType(message.body);
						assignedChannel = channel;
//						rc.setIndicatorString(1, goalLoc.toString());
						return channel;
					}
				}
			}
			
		}
		
//		rc.setIndicatorString(1, "not shield");
		
		for (int i = encampmentJobChannelList.length; --i >= 0; ) {
			channel = encampmentJobChannelList[i];
			message = BroadcastSystem.read(channel);
			
			
//			System.out.println("findJobChannel: " + channel.toString());
//			System.out.println("channelIsValid: " + message.isValid);
//			System.out.println("channelBody: " + message.body);

			if (message.isValid && message.body != maxMessage) {
				onOrOff = parseOnOrOff(message.body);
				isTaken = parseTaken(message.body);
				if (onOrOff == 1 && isTaken == 0) { //if job is on and untaken
					goalLoc = parseLocation(message.body);
					if (rc.canSenseSquare(goalLoc)) {
						GameObject robotOnSquare = rc.senseObjectAtLocation(goalLoc);
						if (robotOnSquare == null || !robotOnSquare.getTeam().equals(rc.getTeam())) {
							assignedRobotType = parseRobotType(message.body);
							assignedChannel = channel;
							return channel;
						}
					} else {
						assignedRobotType = parseRobotType(message.body);
						assignedChannel = channel;
						return channel;
					}
				} else if (onOrOff == 1) {
					int postedRoundNum = parseRoundNum(message.body);
					if ((16+currentRoundNum - postedRoundNum)%16 >= 4) { // it hasn't been written on for 5 turns
						goalLoc = parseLocation(message.body);
//						System.out.println("posted: " + postedRoundNum);
//						System.out.println("current: " + currentRoundNum % 16);
//
						if (rc.canSenseSquare(goalLoc)) {
							GameObject robotOnSquare = rc.senseObjectAtLocation(goalLoc);
							if (robotOnSquare == null || !robotOnSquare.getTeam().equals(rc.getTeam())) {
								assignedRobotType = parseRobotType(message.body);
								assignedChannel = channel;
								return channel;
							}
						} else {
							assignedRobotType = parseRobotType(message.body);
							assignedChannel = channel;
							return channel;
						}
					}
				}

			}
		}
		return null;
	}
	
	/** When a shield is born, they use this to post to the HQ that it's finished
	 * 
	 * @param currLoc
	 */
	public static void postShieldCompletionMessage(MapLocation currLoc) {
		int newmsg = (currLoc.x << 8) + currLoc.y;
		BroadcastSystem.write(shieldCompChannel, newmsg);
	}
	
	/**
	 * Encampments use this at its birth to indicate to the HQ
	 * that the encampment is finished
	 * @param currLoc
	 * @return ChannelType
	 */
	public static void postCompletionMessage(MapLocation currLoc) {
		forloop: for (int i = encampmentCompletionChannelList.length; --i >= 0; ) {
			ChannelType channel = encampmentCompletionChannelList[i];
			Message message = BroadcastSystem.read(channel);
			if (message.isUnwritten || (message.isValid && message.body == maxMessage)) {
				int newmsg = (currLoc.x << 8) + currLoc.y;
				BroadcastSystem.write(channel, newmsg);
				break forloop; 
			}
		}
	}
	
	public static void postUnreachableMessage(MapLocation goalLoc) {
		if (assignedRobotType == RobotType.SHIELDS) {
			Message message = BroadcastSystem.read(shieldCompChannel);
			if (message.isUnwritten || (message.isValid && message.body == maxMessage)){
				int newmsg = (1 << 16) + (goalLoc.x << 8) + goalLoc.y;
//				System.out.println("unreachable msg: " + newmsg);
				BroadcastSystem.write(shieldCompChannel, newmsg);
			}
		} else {
			forloop: for (int i = encampmentCompletionChannelList.length; --i >= 0; ) {
				ChannelType channel = encampmentCompletionChannelList[i];
				Message message = BroadcastSystem.read(channel);
				if (message.isUnwritten || (message.isValid && message.body == maxMessage)){
					int newmsg = (1 << 16) + (goalLoc.x << 8) + goalLoc.y;
//					System.out.println("unreachable msg: " + newmsg);
					BroadcastSystem.write(channel, newmsg);
					break forloop; 
				}
			}
		}
		
	}
	
	/**
	 * In the round after its birth (or 5 after), the encampment "cleans up after itself"
	 * and posts 0 in the channel
	 * @param channel
	 */
	public static void postCleanUp(ChannelType channel) {
		BroadcastSystem.writeMaxMessage(channel);
	}
	
	/**
	 * HQ uses this to see if the shield job has been completed (or is unreachable)
	 * if unreachable, it finds a new shieldlocation and posts the location
	 * 
	 * @return
	 * @throws GameActionException
	 */
	public static EncampmentJobMessageType checkShieldCompletion() throws GameActionException {
		Message message = BroadcastSystem.read(shieldCompChannel);
		if (message.isValid && message.body != maxMessage) {
//			System.out.println("shieldCompChannel: " + message.body);
			int locY = message.body & 0xFF;
			int locX = (message.body >> 8) & 0xFF;
			int unreachableBit = message.body >> 16;
			postCleanUp(shieldCompChannel); // cleanup
			if (unreachableBit == 1) { // if unreachable
				unreachableEncampments.add(new MapLocation(locX, locY));
				numUnreachableEncampments++;
				// update shieldLocation and broadcast it
				setShieldLocation();
				postShieldLocation();
				shieldJobOustanding = false;
//				System.out.println("failure: " + new MapLocation(locX, locY));
				return EncampmentJobMessageType.FAILURE;
				
			} else { // it's a completion message
				shieldJobOustanding = false;
//				System.out.println("completion");
				return EncampmentJobMessageType.COMPLETION;
			}
		}
		return EncampmentJobMessageType.EMPTY;
	}

	/**
	 * HQ uses this to see if a certain channel contains a maplocation
	 * of a certain completed encampment. Returns null if 
	 * @param channel
	 * @return
	 */
	public static EncampmentJobMessageType checkCompletion(ChannelType channel) {
		Message message = BroadcastSystem.read(channel);
		if (message.isValid && message.body != maxMessage) {
			
			int locY = message.body & 0xFF;
			int locX = (message.body >> 8) & 0xFF;
			int unreachableBit = message.body >> 16;
			postCleanUp(channel); // cleanup
//			System.out.println("locy: " + locY + " locx: " + locX);
			if (unreachableBit == 1) { // if unreachable
//				System.out.println("unreachable!!!");
				unreachableEncampments.add(new MapLocation(locY, locX));
				numUnreachableEncampments++;
				return EncampmentJobMessageType.FAILURE;
			} else { // it's a completion message
				return EncampmentJobMessageType.COMPLETION;
			}
		}
		return EncampmentJobMessageType.EMPTY;
	}
	
	/**
	 * HQ uses this to see if the shield job has been completed (or is unreachable)
	 * if unreachable, it finds a new shieldlocation and posts the location
	 * 
	 * @return
	 * @throws GameActionException
	 */
	public static EncampmentJobMessageType checkShieldCompletionOnCycle() throws GameActionException {
		Message message = BroadcastSystem.readLastCycle(shieldCompChannel);
		if (message.isValid && message.body != maxMessage) {
//			System.out.println("shieldCompChannel: " + message.body);
			int locY = message.body & 0xFF;
			int locX = (message.body >> 8) & 0xFF;
			int unreachableBit = message.body >> 16;
			postCleanUp(shieldCompChannel); // cleanup
			if (unreachableBit == 1) { // if unreachable
				unreachableEncampments.add(new MapLocation(locX, locY));
				numUnreachableEncampments++;
				// update shieldLocation and broadcast it
				setShieldLocation();
				postShieldLocation();
				shieldJobOustanding = false;
//				System.out.println("failure: " + new MapLocation(locX, locY));
				return EncampmentJobMessageType.FAILURE;
				
			} else { // it's a completion message
				shieldJobOustanding = false;
//				System.out.println("completion");
				return EncampmentJobMessageType.COMPLETION;
			}
		}
		return EncampmentJobMessageType.EMPTY;
	}
	
	/**
	 * HQ uses this to see if a certain channel contains a maplocation
	 * of a certain completed encampment. Returns null if 
	 * @param channel
	 * @return
	 */
	public static EncampmentJobMessageType checkCompletionOnCycle(ChannelType channel) {
		Message message = BroadcastSystem.readLastCycle(channel);
		if (message.isValid && message.body != maxMessage) {
			
			int locY = message.body & 0xFF;
			int locX = (message.body >> 8) & 0xFF;
			int unreachableBit = message.body >> 16;
//			System.out.println("locy: " + locY + " locx: " + locX);
			if (unreachableBit == 1) { // if unreachable
				unreachableEncampments.add(new MapLocation(locY, locX));
				numUnreachableEncampments++;
				return EncampmentJobMessageType.FAILURE;
			} else { // it's a completion message
				return EncampmentJobMessageType.COMPLETION;
			}
		}
		return EncampmentJobMessageType.EMPTY;
	}
	
	
	/** 
	 * Returns a boolean: true if one of the channels has a 
	 * completion or a failure message, false otherwise
	 * @return
	 */
	public static boolean checkAllCompletion() {
		for (int i = encampmentCompletionChannelList.length; --i >= 0; ) {
			ChannelType channel = encampmentCompletionChannelList[i];
			EncampmentJobMessageType msgType = checkCompletion(channel);
			if (msgType != EncampmentJobMessageType.EMPTY) { // if a completion or a failure message
				return true;
			} 
		}

		return false;
	}
	
	public static boolean checkAllCompletionOnCycle() {
		for (int i = encampmentCompletionChannelList.length; --i >= 0; ) {
			ChannelType channel = encampmentCompletionChannelList[i];
			EncampmentJobMessageType msgType = checkCompletionOnCycle(channel);
			if (msgType != EncampmentJobMessageType.EMPTY) { // if a completion or a failure message
				return true;
			} 
		}
		
		return false;
	}
	
	/**
	 * The HQ calls this at the beginning of every broadcast cycle
	 * to persist old job messages that were untaken
	 */
	public static void persistChannelsOnCycle() {
		for (int i = encampmentChannels.length; --i >= 0; ) { // persist on encampment channels
			ChannelType channel = encampmentChannels[i];
			Message msgLastCycle = BroadcastSystem.readLastCycle(channel);
//			System.out.println("isValid: " + msgLastCycle.isValid);
//			System.out.println("body: " + msgLastCycle.body);

			if (msgLastCycle.isValid && msgLastCycle.body != maxMessage) {
				// check if the job was on and was untaken
				if (parseOnOrOff(msgLastCycle.body) == 1 && parseTaken(msgLastCycle.body) == 0) {
//					System.out.println("hello!!!!!");
//					System.out.println("channel: " + channel.toString());
					int robotTypeToBuild = getRobotTypeToBuild();
					postJobWithoutIncrementing(channel, parseLocation(msgLastCycle.body), robotTypeToBuild);
				} else if (parseOnOrOff(msgLastCycle.body) == 1 && parseTaken(msgLastCycle.body) == 1) {
					int postedRoundNum = parseRoundNum(msgLastCycle.body);
					if ((16+Clock.getRoundNum() - postedRoundNum)%16 >= 4) { // it hasn't been written on for 5 turns
//						System.out.println("hello!!!!!");
//						System.out.println("channel: " + channel.toString());
						int robotTypeToBuild = getRobotTypeToBuild();
						postJobWithoutIncrementing(channel, parseLocation(msgLastCycle.body), robotTypeToBuild);
					}
				}
			}
		}
		
		// persist for shields
		Message msgLastCycle = BroadcastSystem.readLastCycle(shieldChannel);
//		System.out.println("isValid: " + msgLastCycle.isValid);
//		System.out.println("body: " + msgLastCycle.body);

		if (msgLastCycle.isValid && msgLastCycle.body != maxMessage) {
			// check if the job was on and was untaken
			if (parseOnOrOff(msgLastCycle.body) == 1 && parseTaken(msgLastCycle.body) == 0) {
//				System.out.println("hello!!!!!");
//				System.out.println("channel: " + channel.toString());
				int robotTypeToBuild = 3;
				postJobWithoutIncrementing(shieldChannel, parseLocation(msgLastCycle.body), robotTypeToBuild);
			} else if (parseOnOrOff(msgLastCycle.body) == 1 && parseTaken(msgLastCycle.body) == 1) {
				int postedRoundNum = parseRoundNum(msgLastCycle.body);
				if ((16+Clock.getRoundNum() - postedRoundNum)%16 >= 4) { // it hasn't been written on for 5 turns
//					System.out.println("hello!!!!!");
//					System.out.println("channel: " + channel.toString());
					int robotTypeToBuild = getRobotTypeToBuild();
					postJobWithoutIncrementing(shieldChannel, parseLocation(msgLastCycle.body), robotTypeToBuild);
				}
			}
		}
		
	}
	/**
	 * returns new list of channels corresponding to the new jobs
	 * that will maintain the same channels for the old jobs
	 * but find new channels for the new ones
	 * @param newJobsList
	 * @param oldJobsList
	 * @param oldChannelsList
	 * @return
	 */
	public static ChannelType[] assignChannels(MapLocation[] newJobsList, MapLocation[] oldJobsList, ChannelType[] oldChannelsList) {
		ChannelType[] channelList = new ChannelType[maxEncampmentJobs];

		int arrayIndices[] = new int[newJobsList.length];

		int channelIndex = -1;
		// keep old channels for non-new jobs
		for (int i = newJobsList.length; --i >= 0; ) {
			arrayIndices[i] = arrayIndex(newJobsList[i], oldJobsList);
			if (arrayIndices[i] != -1 && newJobsList[i] != null) { // if the job is not new, keep the same channel
				channelList[i] = oldChannelsList[arrayIndices[i]];
			}
		}
		
		// allocate unused channels for new jobs
		for (int i = newJobsList.length; --i >= 0; ) {
			if (arrayIndices[i] == -1 && newJobsList[i] != null) {
				channelLoop: for (ChannelType channel: encampmentJobChannelList) {
					channelIndex = arrayIndex(channel, channelList);
					if (channelIndex == -1) { // if not already used, use it and post job
						channelList[i] = channel;
						int robotTypeToBuild = getRobotTypeToBuild();
						postJob(channel, newJobsList[i], robotTypeToBuild);
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

		for (int i = array.length; --i >= 0; ) {
			if (array[i] != null && array[i].equals(e)) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * checks nearby encampments to see if jobs need to be changed
	 * @throws GameActionException
	 */
	public static void updateJobs() throws GameActionException {
		
//		System.out.println("numEncampmentsNeeded: " + numEncampmentsNeeded);
//		System.out.println("numUnreachableEncampments: " + numUnreachableEncampments);
		
		
		if (DataCache.numAlliedEncampments >= hardEncampmentLimit) {
			numEncampmentsNeeded = 0;
		}
//		System.out.println("Before update: " + Clock.getBytecodeNum());
		MapLocation[] neutralEncampments;
		if (robot.strategy == Strategy.NUKE){
			neutralEncampments = getPossibleArtilleryLocations();
		} else {
			neutralEncampments = rc.senseEncampmentSquares(DataCache.ourHQLocation, 10000, Team.NEUTRAL);
		}
		if (numEncampmentsNeeded > neutralEncampments.length){
			numEncampmentsNeeded = neutralEncampments.length;
		}
		
		if (numEncampmentsNeeded != 0) {
			MapLocation[] newJobsList = getClosestMapLocations(DataCache.ourHQLocation, neutralEncampments, numEncampmentsNeeded);
//			System.out.println("new jobs list: " + Clock.getBytecodeNum());

			ChannelType[] channelList = EncampmentJobSystem.assignChannels(newJobsList, EncampmentJobSystem.encampmentJobs, EncampmentJobSystem.encampmentChannels);

			for (int i = numEncampmentsNeeded; --i >= 0; ) { // update lists
				EncampmentJobSystem.encampmentJobs[i] = newJobsList[i];
//				System.out.println("encampmentJobs.x: " + encampmentJobs[i].x);
//				System.out.println("encampmentJobs.y: " + encampmentJobs[i].y);

				EncampmentJobSystem.encampmentChannels[i] = channelList[i];
			}

//			System.out.println("update lists: " + Clock.getBytecodeNum());

			// clear unused channels
			for (int i = encampmentJobChannelList.length; --i >= 0; ) {
				ChannelType channel = encampmentJobChannelList[i];
				if (arrayIndex(channel, EncampmentJobSystem.encampmentChannels) == -1) { // if unused
//					System.out.println("channel overwrite: " + channel.toString());
					BroadcastSystem.writeMaxMessage(channel); // reset the channel
				}
			}
		}
//		System.out.println("After update: " + Clock.getRoundNum());
	}

	/**
	 * HQ uses this to check if any jobs are completed, and then updates new jobs
	 * @throws GameActionException
	 */
	public static void updateJobsAfterChecking() throws GameActionException {
		if (EncampmentJobSystem.checkAllCompletion()) { // if it contains non-null elements
			updateJobs();
		}
	}
	
	public static void updateJobsOnCycle() throws GameActionException {
//		System.out.println("callpersist");
		persistChannelsOnCycle();
		checkAllCompletionOnCycle();
		updateJobs();
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
		for (int i = allLoc.length; --i >= 0; ) {
			allDistances[i] = origin.distanceSquaredTo(allLoc[i]);
		}

		int[] allLocIndex = new int[allLoc.length];
		
		int runningDist = 1000000;
		MapLocation runningLoc = null;
		int runningIndex = 0;
		for (int j = k; --j >= 0; ) {
			runningDist = 1000000;
			for (int i = allLoc.length; --i >= 0; ) {
				if (allDistances[i] < runningDist && allLocIndex[i] == 0 && !unreachableEncampments.contains(allLoc[i])) { 
					// if distances are smaller and it's not unreachable
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
	
	
	public static int getRobotTypeToBuild() {
		if (robot.strategy == Strategy.NUKE) {
			return 2; // artillery
		} else {
			if (supCount == 0 && genCount == 0) {
				return 0; // supplier
			}
			if (((double) supCount)/(supCount + genCount) > 0.8) {
				return 1; // generator
			} else {
				return 0; // supplier
			}
		}
		
	}
	
	public static void setShieldLocation() throws GameActionException {
//		System.out.println("setshield");
		shieldsLoc = getShieldLocation();
	}
	
	/** finds a possible shield location
	 * 
	 * @return
	 * @throws GameActionException
	 */
	public static MapLocation getShieldLocation() throws GameActionException {
		MapLocation[] possibleShieldLocations = rc.senseEncampmentSquares(DataCache.ourHQLocation, DataCache.rushDistSquared/4, null); 
		int x1 = DataCache.ourHQLocation.x;
		int y1 = DataCache.ourHQLocation.y;
		int x2 = DataCache.enemyHQLocation.x;
		int y2 = DataCache.enemyHQLocation.y;
		
		double lineA;
		double lineB;
		double lineC;
		if (x2 != x1) {
			lineA = (double)(y2-y1)/(x2-x1);
			lineB = -1;
			lineC = y1 - lineA * x1;
		} else { // x = x_1 \implies 1 * x + 0 * y - x_1 = 0
			lineA = 1;
			lineB = 0;
			lineC = -x1;
		}
		
		double lineDistanceDenom = Math.sqrt(lineA*lineA + lineB*lineB);
		
		double distanceToLine;		
		
		
		for (MapLocation shieldLoc : possibleShieldLocations) {
//			System.out.println(shieldLoc);
			if (!unreachableEncampments.contains(shieldLoc) && shieldLoc.distanceSquaredTo(DataCache.enemyHQLocation) <= DataCache.rushDistSquared/2) {
				distanceToLine = Math.abs(lineA * shieldLoc.x + lineB * shieldLoc.y + lineC) / lineDistanceDenom;
				if (distanceToLine < 10) {
//					System.out.println("new loc: " + shieldLoc);
//					rc.setIndicatorString(0, shieldLoc.toString());
					return shieldLoc;
				}
			}
		}
		
		for (MapLocation shieldLoc : possibleShieldLocations) {
			
			if (!unreachableEncampments.contains(shieldLoc) && shieldLoc.distanceSquaredTo(DataCache.enemyHQLocation) <= DataCache.rushDistSquared) {
				distanceToLine = Math.abs(lineA * shieldLoc.x + lineB * shieldLoc.y + lineC) / lineDistanceDenom;
				if (distanceToLine < 10) {
//					System.out.println("new loc2: " + shieldLoc);
//					rc.setIndicatorString(0, shieldLoc.toString());
					return shieldLoc;
				}
			}
		}
		
//		rc.setIndicatorString(0, "null");
		
		return null;
		
		
	}
	
	/**
	 * For nuke bot, this gets the center of the circle in which we build artillery in
	 * @return
	 */
	public static MapLocation getArtilleryCenter() {
		int dx = DataCache.enemyHQLocation.x - DataCache.ourHQLocation.x;
		int dy = DataCache.enemyHQLocation.y - DataCache.ourHQLocation.y;
		
		double vectorMag = Math.sqrt(dx*dx + dy*dy);
		double dxNorm = dx/vectorMag;
		double dyNorm = dy/vectorMag;
		
		int centerx = (int) (DataCache.ourHQLocation.x + 6 * dxNorm);
		int centery = (int) (DataCache.ourHQLocation.y + 6 * dyNorm);
		
//		rc.setIndicatorString(2, new MapLocation(centerx, centery).toString());
		
		return new MapLocation(centerx, centery);
	}
	
	/**
	 * Gets list of maplocations on which we can build artillery on
	 * @return
	 * @throws GameActionException
	 */
	public static MapLocation[] getPossibleArtilleryLocations() throws GameActionException {
		MapLocation artCenter = getArtilleryCenter();
		
//		System.out.println("getArtilleryCenter: " + artCenter);
//		System.out.println("rushDistSquared/25: " + DataCache.rushDistSquared/25);
//		int encampmentRadius = (int) (DataCache.rushDistSquared/25 + 6 * Math.sqrt(DataCache.rushDistSquared)/5 + 9);
		int encampmentRadiusSquared = 72;
		return rc.senseEncampmentSquares(artCenter, encampmentRadiusSquared, Team.NEUTRAL);
	}

	/**
	 * Creates a 24-bit job message to send from the goal location
	 * @param goalLoc
	 * @return
	 */
	public static int createMessage(MapLocation goalLoc, boolean isTaken, boolean onOrOff, int robType) {
		int msg = robType << 22;
		
		msg += (goalLoc.x << 14);
		msg += (goalLoc.y << 6);
		if (isTaken) {
			msg += 0x20;
		}
		if (onOrOff) {
			msg += 0x10;
		}
		msg += Clock.getRoundNum() % 16;
		
		return msg;
	}
	
	/**
	 * creates shield message with the location of the shield
	 * @param shieldLoc
	 * @return
	 */
	public static int createShieldLocMessage(MapLocation shieldLoc) {
		int msg = shieldLoc.x;
		msg += (shieldLoc.y << 8);
		return msg;
	}
	/** parses message in the channel SHIELD_LOCATION
	 * 
	 * @param msgBody
	 * @return
	 */
	public static MapLocation parseShieldLocation(int msgBody) {
		int x = msgBody & 0xFF;
		int y = (msgBody >> 8) & 0xFF;
		return new MapLocation(x,y);
	}
	/**
	 * Parses a 24-bit job-message
	 * @param msgBody
	 * @return an array containing round mod 16, onOrOff, job taken or not, y-coord, x-coord, and encampment type
	 */
	public static int[] parseEntireJobMessage(int msgBody) {
		int[] output = new int[6];
		output[0] = msgBody & 0xF; // round # mod 16
		output[1] = (msgBody >> 4) & 0x1; // on or off
		output[2] = (msgBody >> 5) & 0x1; // job taken or not
		output[3] = (msgBody >> 6) & 0xFF; // y-coord of location
		output[4] = (msgBody >> 14) & 0xFF; // x-coord of location
		output[5] = (msgBody >> 22);
		
		return output;
	}
	
	public static int parseRoundNum(int msgBody) { // round number mod 16
		return msgBody & 0xF;
	}
	
	public static int parseOnOrOff(int msgBody) { // job on or off
		return (msgBody >> 4) & 0x1;
	}
	
	public static int parseTaken(int msgBody) { // job taken or not
		return (msgBody >> 5) & 0x1; 
	}
	
	public static MapLocation parseLocation(int msgBody) {
		int y = (msgBody >> 6) & 0xFF; // y-coord of location
		int x = (msgBody >> 14) & 0xFF; // x-coord of location
		return new MapLocation(x,y);
	}
	
	public static RobotType parseRobotType(int msgBody) {
		int robotTypeInt = msgBody >> 22;
		if (robotTypeInt == 0) {
			return RobotType.SUPPLIER;
		} else if (robotTypeInt == 1) {
			return RobotType.GENERATOR;
		} else if (robotTypeInt == 2) {
			return RobotType.ARTILLERY;
		} else { // 3
			return RobotType.SHIELDS;
		}
	}
	
//	public static void main(String[] arg0) {
//		int[] output = parseEntireJobMessage(0b0000111100001110011111);
//		System.out.println("parsed: " + output[0]);
//		System.out.println("parsed: " + output[1]);
//		System.out.println("parsed: " + output[2]);
//		System.out.println("parsed: " + output[3]);
//		System.out.println("parsed: " + output[4]);
//		System.out.println("parsed: " + output[5]);
//
//		
//		System.out.println("message: " + createMessage(new MapLocation(15,14), false, true, 0));
//	}
}
