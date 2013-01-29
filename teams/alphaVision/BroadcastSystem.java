package alphaVision;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import battlecode.common.Clock;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;

/**
 * BroadcastSystem for keeping track of BroadcastChannels. Robots
 * can query this system for getting instances of BroadcastChannels
 * to write and read.
 */
public class BroadcastSystem {
	
	public static BaseRobot robot;
	public static RobotController rc;
	public static byte signature = 0x4C; // TODO: Better signature verification (based on round number, channel type, etc.)
	public static final int signatureMask = 0x00FFFFFF;
	
	/**
	 * Initializes BroadcastSystem by setting rc
	 * @param myRobot
	 */
	public static void init(BaseRobot myRobot) {
		robot = myRobot;
		rc = robot.rc;
	}
	
	/**
	 * Reads a message on channelType. Checks if signature is correct.
	 * @param channelType
	 * @return
	 */
	public static Message read(ChannelType channelType) {
		try {
			if (rc != null) {
				for (int channelNo : getChannelNos(channelType)) {
					int rawMessage = rc.readBroadcast(channelNo);
					if (rawMessage == 0) {
						return new Message(false, true);
					}
					byte testSignature = (byte)(rawMessage >> 24);
					if (signature == testSignature) { // verified
						int body = rawMessage & signatureMask;
						return new Message(body, true, false); // true means message is valid
					}
				}
			}
			return new Message(false, false);
		} catch (Exception e) {
			return new Message(false, false);
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
				for (int channelNo : getChannelNos(channelType)) {
					rc.broadcast(channelNo, result);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Use hashing of the current time and channelType to calculate what channels to use
	 * @param channelType
	 * @return channelNos
	 */
	public static int[] getChannelNos(ChannelType channelType) {
		int round = Clock.getRoundNum();
		int round_cycle = round / Constants.CHANNEL_CYCLE;		
		return getChannelNos(channelType, round_cycle);
	}
	
	/**
	 * Use hashing of the current time and channelType to calculate what channels to use (from the last cycle)
	 * @param channelType
	 * @return
	 */
	public static int[] getChannelNosLastCycle(ChannelType channelType) {
		int round = Clock.getRoundNum();
		int round_cycle = round / Constants.CHANNEL_CYCLE - 1;		
		return getChannelNos(channelType, round_cycle);
	}
	
	/**
	 * For precomputed channels.
	 * @param channelType
	 * @param round_cycle
	 * @return
	 */
	public static int[] getChannelNosPrecomputed(ChannelType channelType, int round_cycle) {
		return PrecomputedChannelNos.precomputedChannelNos[round_cycle][channelType.ordinal()];
//		return new int[0];
	}
	
	public static Message readLastCycle(ChannelType channelType) {
		try {
			if (rc != null) {
				for (int channelNo : getChannelNosLastCycle(channelType)) {
					int rawMessage = rc.readBroadcast(channelNo);
					if (rawMessage == 0) {
						return new Message(false, true);
					}
					byte testSignature = (byte)(rawMessage >> 24);
					if (signature == testSignature) { // verified
						int body = rawMessage & signatureMask;
						return new Message(body, true, false); // true means message is valid
					}
				}
			}
			return new Message(false, false);
		} catch (Exception e) {
			return new Message(false, false);
		}
	}
	
	/**
	 * Writes constant.MAX_MESSAGE into the channel
	 * @param channelType
	 */
	public static void writeMaxMessage(ChannelType channelType) {
		write(channelType, Constants.MAX_MESSAGE);
	}
	
	public static int[] getChannelNos(ChannelType channelType, int constant) {
//		if (constant < Constants.MAX_PRECOMPUTED_ROUNDS / Constants.CHANNEL_CYCLE) {
//			return getChannelNosPrecomputed(channelType, constant);
//		}
		int[] channelNos = new int[Constants.REDUNDANT_CHANNELS];
		int rangeStart = channelType.ordinal() * ChannelType.range;
		constant += 1;
		for (int i = 0; i < Constants.REDUNDANT_CHANNELS; i++) {
			int offset = ((Integer.toString(((constant << 4 + 17 * channelType.ordinal()) << 4 + i)).hashCode())+rc.getTeam().ordinal()) % ChannelType.range;
			// ensure that the offset is nonnegative
			if (offset < 0) {
				offset += ChannelType.range;
			}
			channelNos[i] = rangeStart + offset;
		}
		return channelNos;
	}	
	

	
	
	
	
	
	
	
	// All code below this point is for pre-computing channels
	
//	/**
//	 * Right now, we're using this for pre-generating a list of channels so we don't have to calculate them during the game.
//	 * This writes to PrecomputedChannelNos.java
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		try {
//			String filename = System.getProperty("user.dir") + "\\teams\\baseTurgid\\PrecomputedChannelNos.java";
//			FileWriter fw = new FileWriter(filename);
//			BufferedWriter bw = new BufferedWriter(fw, 100000000);
//			
//			bw.write("package baseTurgid;\n\n");
//			bw.write("public class PrecomputedChannelNos {\n\n");
//			bw.write("public static int[][][] precomputedChannelNos =\n");
//			bw.write("\t{");
//			for (int constant = 0; constant < Constants.MAX_PRECOMPUTED_ROUNDS / Constants.CHANNEL_CYCLE; constant++) {
//				if (constant > 0) {
//					bw.write("\t");
//				}
//				bw.write("{");
//				for (int channel = 0; channel < ChannelType.size; channel++) {
//					int[] channelNos = getChannelNos(ChannelType.values()[channel], constant);
//					bw.write("{");
//					for (int i = 0; i < channelNos.length; i++) {
//						bw.write(Integer.toString(channelNos[i]));
//						if (i < channelNos.length - 1) {
//							bw.write(", ");
//						}
//					}
//					bw.write("}");
//					if (channel < ChannelType.size - 1) {
//						bw.write(", ");
//					}
//				}
//				bw.write("}");
//				if (constant < GameConstants.ROUND_MAX_LIMIT - 1) {
//					bw.write(", ");
//				}
//				
//				bw.write("\n");
//			}
//			bw.write("};\n\n");
//			bw.write("}\n");
//			bw.flush();
//			bw.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
}
