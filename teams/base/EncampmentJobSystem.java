package base;

import battlecode.common.Clock;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class EncampmentJobSystem {
	public static BaseRobot robot;
	public static RobotController rc;
	public static MapLocation goalLoc;
	public static ChannelType[] encampmentJobChannelList = 
		{ChannelType.CHANNELENC1,
		ChannelType.CHANNELENC2,
		ChannelType.CHANNELENC3,
		ChannelType.CHANNELENC4,
		ChannelType.CHANNELENC5,
		ChannelType.CHANNELENC6,
		ChannelType.CHANNELENC7,
		ChannelType.CHANNELENC8}; // list of encampment channels

	public static ChannelType[] encampmentCompletionChannelList=
		{ChannelType.CHANNELCOMP1,
		ChannelType.CHANNELCOMP2,
		ChannelType.CHANNELCOMP3,
		ChannelType.CHANNELCOMP4,
		ChannelType.CHANNELCOMP5,
		ChannelType.CHANNELCOMP6,
		ChannelType.CHANNELCOMP7,
		ChannelType.CHANNELCOMP8}; // list of encampment completion channels
	
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
	 * @param channel
	 */
	public static void updateJobTaken(ChannelType channel) {
		BroadcastSystem.write(channel, createMessage(goalLoc, true, true));
	}
	
	/**
	 * Soldiers use this to find an untaken job or a taken job that has been inactive
	 * @return ChannelType channel
	 */
	public static ChannelType findJob() {
		for (ChannelType channel: encampmentJobChannelList) {
			Message message = BroadcastSystem.read(channel);
			if (message.isValid) {
				rc.setIndicatorString(0, Integer.toString(message.body));
				int onOrOff = parseOnOrOff(message.body);
				int isTaken = parseTaken(message.body);
				if (onOrOff == 1 && isTaken == 0) { //if job is on and untaken
					goalLoc = parseLocation(message.body);
					return channel;
				} else if (onOrOff == 1) {
					int roundNum = parseRoundNum(message.body);
					if ((4+Clock.getRoundNum() - roundNum)%4 >= 2) { // it hasn't been written on for 2 turns
						goalLoc = parseLocation(message.body);
						return channel;
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
	 */
	public static void postCompletionMessage(MapLocation currLoc) {
		for (ChannelType channel: encampmentCompletionChannelList) {
			Message message = BroadcastSystem.read(channel);
			if (message.isUnwritten || (message.isValid && message.body == 0xFFFFFF)){
				int newmsg = (currLoc.x << 8) + currLoc.y;
				BroadcastSystem.write(channel, newmsg);
			}
		}
	}
	
	/**
	 * In the round after its birth (or 5 after), the encampment "cleans up after itself"
	 * and posts 0 in the channel
	 * @param channel
	 */
	public static void postCleanUp(ChannelType channel) {
		BroadcastSystem.write(channel, 0xFFFFFF);
	}
	
	/**
	 * HQ uses this to see if a certain channel contains a maplocation
	 * of a certain completed encampment. Returns null if 
	 * @param channel
	 * @return
	 */
	public static MapLocation checkCompletion(ChannelType channel) {
		Message message = BroadcastSystem.read(channel);
		if (message.isValid && message.body != 0xFFFFFF) {
			int locY = message.body & 0xFF;
			int locX = message.body >> 8;
			return new MapLocation(locY, locX);
		}
		return null;
	}
	
	/**
	 * Creates a 20-bit job message to send from the goal location
	 * @param goalLoc
	 * @return
	 */
	public static int createMessage(MapLocation goalLoc, boolean isTaken, boolean onOrOff) {
		int msg = (goalLoc.x << 12);
		msg += (goalLoc.y << 4);
		if (isTaken) {
			msg += 0x8;
		}
		if (onOrOff) {
			msg += 0x4;
		}
		msg += Clock.getRoundNum() % 4;
		
		return msg;
	}
	
	/**
	 * Parses a 20-bit job-message
	 * @param msgBody
	 * @return an array containing round mod 4, onOrOff, job taken or not, y-coord, and x-coord
	 */
	public static int[] parseEntireJobMessage(int msgBody) {
		int[] output = new int[5];
		output[0] = msgBody & 0x3; // round # mod 4
		output[1] = (msgBody >> 2) & 0x1; // on or off
		output[2] = (msgBody >> 3) & 0x1; // job taken or not
		output[3] = (msgBody >> 4) & 0xFF; // y-coord of location
		output[4] = (msgBody >> 12) & 0xFF; // x-coord of location
		
		return output;
	}
	
	public static int parseRoundNum(int msgBody) { // round number mod 4
		return msgBody & 0x3;
	}
	
	public static int parseOnOrOff(int msgBody) { // job on or off
		return (msgBody >> 2) & 0x1;
	}
	
	public static int parseTaken(int msgBody) { // job taken or not
		return (msgBody >> 3) & 0x1; 
	}
	
	public static MapLocation parseLocation(int msgBody) {
		int y = (msgBody >> 4) & 0xFF; // y-coord of location
		int x = (msgBody >> 12) & 0xFF; // x-coord of location
		return new MapLocation(x,y);
	}
}
