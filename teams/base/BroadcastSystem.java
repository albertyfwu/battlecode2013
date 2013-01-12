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
	public static final int signatureMask = 0x00FFFFFF;
	
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
						int body = rawMessage & signatureMask;
						return new Message(body, true); // true means message is valid
					}
				}
			}
			return new Message(false);
		} catch (Exception e) {
			return new Message(false);
		}
	}
	
	/**
	 * Writes a message to channelType.
	 * WARNING: Only can use 24 low-order bits from the body
	 * @param channelType
	 * @param header
	 * @param body
	 */
	public static void write(ChannelType channelType, int body) {
		if (rc != null) {
			int result = (signature << 24) + (signatureMask & body);
			try {
				for (int channelNo : getChannelNos(channelType)){
					rc.broadcast(channelNo, result);
				}
			} catch (Exception e) {
				//
			}
		}
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
