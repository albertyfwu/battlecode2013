package base;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.GameObject;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;

/**
 * TODO: make the HQ update every cycle call, but have a modified update
 * where we check the previous cycle's channels to see if any jobs weren't taken
 * 
 * if any jobs were not taken, then copy the message over to the new channels
 * 
 * Also check whether or not this repair fixes the other issue with the 
 * completion channel half of this problem. Previous solution was to update every time it spawns.
 * @author asdfryan
 *
 */
public class EncampmentJobSystem {
	public static BaseRobot robot;
	public static RobotController rc;
	public static MapLocation goalLoc;
	
	public static ChannelType[] encampmentJobChannelList = 
		{ChannelType.ENC1,
		ChannelType.ENC2,
		ChannelType.ENC3,
		ChannelType.ENC4,
		ChannelType.ENC5}; // list of encampment channels

	public static ChannelType[] encampmentCompletionChannelList=
		{ChannelType.COMP1,
		ChannelType.COMP2,
		ChannelType.COMP3,
		ChannelType.COMP4,
		ChannelType.COMP5}; // list of encampment completion channels
	
	public static int maxEncampmentJobs = encampmentJobChannelList.length;
	public static int maxMessage = Constants.MAX_MESSAGE; //24-bit message all 1s
	
	
	
	/**
	 * Initializes BroadcastSystem by setting rc
	 * @param myRobot
	 */
	public static void init(BaseRobot myRobot) {
		robot = myRobot;
		rc = robot.rc;
	}
	
	/**
	 * HQ uses this to post a new job
	 * @param channel
	 * @param loc
	 */
	public static void postJob(ChannelType channel, MapLocation loc) {
		BroadcastSystem.write(channel, createMessage(loc, false, true));
	}
	
	/**
	 * Soldiers use this to update the job to claim that it's taken
	 * They also use this every turn to ping back to others
	 * @param channel
	 */
	public static void updateJobTaken(ChannelType channel) {
		BroadcastSystem.write(channel, createMessage(goalLoc, true, true));
	}
	
	/**
	 * Soldiers use this to find an untaken job or a taken job that has been inactive
	 * @return ChannelType channel
	 * @throws GameActionException 
	 */
	public static ChannelType findJob() throws GameActionException {
		int currentRoundNum = Clock.getRoundNum();
		for (ChannelType channel: encampmentJobChannelList) {
			System.out.println("findChannel: " + channel);
			Message message = BroadcastSystem.read(channel);
			if (message.isValid && message.body != maxMessage) {
				rc.setIndicatorString(0, Integer.toString(message.body));
				int onOrOff = parseOnOrOff(message.body);
				int isTaken = parseTaken(message.body);
				if (onOrOff == 1 && isTaken == 0) { //if job is on and untaken
					goalLoc = parseLocation(message.body);
					if (rc.canSenseSquare(goalLoc)) {
						GameObject robotOnSquare = rc.senseObjectAtLocation(goalLoc);
						if (robotOnSquare == null || !robotOnSquare.getTeam().equals(rc.getTeam())) {
							return channel;
						}
					} else {
						return channel;
					}
				} else if (onOrOff == 1) {
					int postedRoundNum = parseRoundNum(message.body);
					if ((16+currentRoundNum - postedRoundNum)%16 >= 4) { // it hasn't been written on for 5 turns
						goalLoc = parseLocation(message.body);
						System.out.println("posted: " + postedRoundNum);
						System.out.println("current: " + currentRoundNum % 16);

						if (rc.canSenseSquare(goalLoc)) {
							GameObject robotOnSquare = rc.senseObjectAtLocation(goalLoc);
							if (robotOnSquare == null || !robotOnSquare.getTeam().equals(rc.getTeam())) {
								return channel;
							}
						} else {
							return channel;
						}

					}
				}

			}
		}
		return null;
	}
	
	/**
	 * Encampments use this at its birth to indicate to the HQ
	 * that the encampment is finished
	 * @param currLoc
	 * @return ChannelType
	 */
	public static ChannelType postCompletionMessage(MapLocation currLoc) {
		for (ChannelType channel: encampmentCompletionChannelList) {
			Message message = BroadcastSystem.read(channel);
			if (message.isUnwritten || (message.isValid && message.body == maxMessage)){
				int newmsg = (currLoc.x << 8) + currLoc.y;
				BroadcastSystem.write(channel, newmsg);
				return channel;
			}
		}
		return null;
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
	 * HQ uses this to see if a certain channel contains a maplocation
	 * of a certain completed encampment. Returns null if 
	 * @param channel
	 * @return
	 */
	public static MapLocation checkCompletion(ChannelType channel) {
		Message message = BroadcastSystem.read(channel);
		if (message.isValid && message.body != maxMessage) {
			
			int locY = message.body & 0xFF;
			int locX = message.body >> 8;
			System.out.println("locy: " + locY + " locx: " + locX);
			return new MapLocation(locY, locX);
		}
		return null;
	}
	
	/** 
	 * Returns list of map locations of encampment jobs that have been completed
	 * IMPORTANT: Last element of the output returns a "map location" whose x and y values
	 * corresponding to number of non-null elements
	 * @return
	 */
	public static MapLocation[] checkAllCompletion() {
		MapLocation[] output = new MapLocation[encampmentCompletionChannelList.length+1];
		int currIndex = 0;
		for (ChannelType channel: encampmentCompletionChannelList) {
			MapLocation loc = checkCompletion(channel);
			if (loc != null) {
				output[currIndex] = loc;
				currIndex++;
			} 
		}
		output[encampmentCompletionChannelList.length] = new MapLocation(currIndex, currIndex);

		return output;
	}
	
	/**
	 * Creates a 22-bit job message to send from the goal location
	 * @param goalLoc
	 * @return
	 */
	public static int createMessage(MapLocation goalLoc, boolean isTaken, boolean onOrOff) {
		int msg = (goalLoc.x << 14);
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
	 * Parses a 22-bit job-message
	 * @param msgBody
	 * @return an array containing round mod 16, onOrOff, job taken or not, y-coord, and x-coord
	 */
	public static int[] parseEntireJobMessage(int msgBody) {
		int[] output = new int[5];
		output[0] = msgBody & 0xF; // round # mod 16
		output[1] = (msgBody >> 4) & 0x1; // on or off
		output[2] = (msgBody >> 5) & 0x1; // job taken or not
		output[3] = (msgBody >> 6) & 0xFF; // y-coord of location
		output[4] = (msgBody >> 14) & 0xFF; // x-coord of location
		
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
	
//	public static void main(String[] arg0) {
//		int[] output = parseEntireJobMessage(0b0000111100001110011111);
//		System.out.println("parsed: " + output[0]);
//		System.out.println("parsed: " + output[1]);
//		System.out.println("parsed: " + output[2]);
//		System.out.println("parsed: " + output[3]);
//		System.out.println("parsed: " + output[4]);
//		
//		System.out.println("message: " + createMessage(new MapLocation(15,14), false, true));
//	}
}
