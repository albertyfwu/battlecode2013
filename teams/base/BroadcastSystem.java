package base;

import battlecode.common.Clock;
import battlecode.common.RobotController;

/**
 * BroadcastSystem for keeping track of BroadcastChannels. Robots
 * can query this system for getting instances of BroadcastChannels
 * to write and read.
 */
public class BroadcastSystem {
	
	public static RobotController rc;
	public static byte signature = 0x3D; // TODO: Better signature verification (based on round number, channel type, etc.)
	
	/**
	 * Initializes BroadcastSystem by setting rc
	 * @param myRC
	 */
	public static void init(RobotController myRC) {
		rc = myRC;
	}
	
	public static Message read(ChannelType channelType) {
		// TODO: Add caching of messages
		try {
			if (rc != null) {
				for (int channelNo : getChannelNos(channelType)) {
					int rawMessage = rc.readBroadcast(channelNo);
					byte testSignature = (byte)(rawMessage >> 24);
					if (signature == testSignature) { // verified
						byte header = (byte)(rawMessage >> 16);
						short body = (short)rawMessage;
						return new Message(header, body, true); // true means message is valid
					}
				}
			}
			return new Message(false);
		} catch (Exception e) {
			return new Message(false);
		}
	}
	
	/**
	 * Writes a message to channelType
	 * @param channelType
	 * @param header
	 * @param body
	 */
	public static void write(ChannelType channelType, byte header, short body) {
		if (rc != null) {
			int result = (((signature << 8) + header) << 16) + body;
			try {
				for (int channelNo : getChannelNos(channelType)){
					rc.broadcast(channelNo, result);
				}
			} catch (Exception e) {
				//
			}
		}
	}
	
	public static void write(ChannelType channelType, Message message) {
		write(channelType, message.header, message.body);
	}
	
	/**
	 * Use hashing of the current time and channelType to calculate what channels to use
	 * @param channelType
	 * @return channelNos
	 */
	public static int[] getChannelNos(ChannelType channelType) {
		int[] channelNos = new int[TeamConstants.REDUNDANT_CHANNELS];
		int rangeStart = channelType.ordinal() * ChannelType.range;
		int constant = Clock.getRoundNum() / TeamConstants.CHANNEL_CYCLE;
		for (int i = 0; i < TeamConstants.REDUNDANT_CHANNELS; i++) {
			int offset = ((channelType.ordinal() * i) ^ constant) % ChannelType.range;
			// ensure that the offset is nonnegative
			if (offset < 0) {
				offset += ChannelType.range;
			}
			channelNos[i] = rangeStart + offset;
		}
		return channelNos;
	}
}
