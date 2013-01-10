package base;

import battlecode.common.Clock;
import battlecode.common.RobotController;

/**
 * BroadcastChannel for broadcasting.
 * 
 * 1. Reserve the first 8 bits for signature.
 * 2. Reserve the next 8 bits for a header.
 * 3. Reserve the last 16 bits for a message.
 *
 * TODO: What if enemies corrupt a single bit?
 */
public class BroadcastChannel {

	int cachedMessageRoundNum = 0;
	Message cachedMessage = null;
	ChannelType channelType;
	byte signature = 0x3D; // TODO: change this later? Base it on the round number as well
	
	public BroadcastChannel(ChannelType channelType) {
		this.channelType = channelType;
	}
	
	// TODO: implement signing / hash check?	
	public void write(RobotController rc, byte header, short body) {
		byte signature = this.signature;
		int result = (((signature << 8) + header) << 16) + body;
		try {
			for (int channelNo : BroadcastSystem.getChannelNos(this.channelType)) {
				rc.broadcast(channelNo, result);
			}
		} catch (Exception e) {
			//
		}
	}
	
	public void write(RobotController rc, Message message) {
		byte header = message.header;
		short body = message.body;
		write(rc, header, body);
	}
	
	public void write(RobotController rc, int header, int body) {
		write(rc, (byte)header, (short)body);
	}
	
	public void write(RobotController rc, short body) {
		write(rc, (byte)0, body);
	}
	
	public void write(RobotController rc, int body) {
		write(rc, (short)body);
	}
	
	// TODO: more redundancy and stuffs
	public Message read(RobotController rc) {
		try {
			if (Clock.getRoundNum() > cachedMessageRoundNum) {
				for (int channelNo : BroadcastSystem.getChannelNos(this.channelType)) {
					int rawMessage = rc.readBroadcast(channelNo);
					byte signature = (byte) (rawMessage >> 24);
					if (this.signature == signature) { // verified
						// TOOD: implement better signature / verification system
						byte header = (byte) (rawMessage >> 16);
						short body = (short) rawMessage;
						cachedMessage = new Message(header, body);
						return cachedMessage;
					}
				}
				return null;
			} else {
				return cachedMessage;
			}
		} catch (Exception e) {
			return null;
		}
	}
	
	public short readBody(RobotController rc) {
		try {
			read(rc);
			if (cachedMessage != null) {
				return cachedMessage.body;
			} else {
				return 0;
			}
		} catch (Exception e) {
			return 0;
		}
	}
	
	/**
	 * For testing purposes.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		int x = 0xFFFFFFFF;
		byte y = (byte) (x >> 16);
		System.out.println(y);
	}
}
