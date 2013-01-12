package macroRallyNav;

import battlecode.common.Clock;
import battlecode.common.GameConstants;

/**
 * BroadcastSystem for keeping track of BroadcastChannels. Robots
 * can query this system for getting instances of BroadcastChannels
 * to write and read.
 */
public class BroadcastSystem {	
	
	public static BroadcastChannel[] broadcastChannels;	
	static {
		ChannelType[] channelTypes = ChannelType.values();
		broadcastChannels = new BroadcastChannel[channelTypes.length];
		for (int i = 0; i < channelTypes.length; i++) {
			broadcastChannels[i] = new BroadcastChannel(channelTypes[i]);
		}
	}
	
	public static BroadcastChannel getChannelByType(ChannelType channelType) {
		return broadcastChannels[channelType.ordinal()];
	}
	
	/**
	 * Use hashing of the current time and channelType to calculate what channels to use
	 * 
	 * @param channelType
	 * @return result
	 */
	public static int[] getChannelNos(ChannelType channelType, int teamID) {
		int[] result = new int[TeamConstants.REDUNDANT_CHANNELS];
		int roundNum = Clock.getRoundNum();
		int mod = GameConstants.BROADCAST_MAX_CHANNELS / ChannelType.size;
		int bigOffset = channelType.ordinal() * mod;
		for (int i = 0; i < TeamConstants.REDUNDANT_CHANNELS; i++) {
			int offset = (((channelType.hashCode() * i) ^ (roundNum / (GameConstants.ROUND_MAX_LIMIT / TeamConstants.CHANNEL_CYCLE_FREQ))) + teamID) % mod;
			if (offset < 0) {
				offset += mod;
			}
			result[i] = offset + bigOffset;
		}
		return result;
	}
	
	// For debugging
	public static void main(String[] args) {
		System.out.println("bye");
		System.out.println((-3) % 10);
	}	
}
