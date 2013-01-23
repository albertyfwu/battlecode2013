package makeOneGuy;

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

	ChannelType channelType;
	byte signature = 0x3D; // TODO: change this later? Base it on the round number as well
	
	public BroadcastChannel(ChannelType channelType) {
		this.channelType = channelType;
	}
	
	// TODO: implement signing / hash check?
	public void write(RobotController rc, Message message) {
		byte signature = this.signature;
		byte header = message.header;
		short body = message.body;
		int result = (((signature << 8) + header) << 16) + body;
		try {
			for (int channelNo : BroadcastSystem.getChannelNos(this.channelType)) {
				

//				System.out.println("result-read: " + (short)(0x0000FFFF & rc.readBroadcast(0)));
				rc.broadcast(channelNo, result);
//				System.out.println("result: " + (short)(result));
				
				
			}
		} catch (Exception e) {
			//
		}
	}
	
	// TODO: more redundancy and stuffs
	public Message read(RobotController rc) {
		try {
			for (int channelNo : BroadcastSystem.getChannelNos(this.channelType)) {
				int rawMessage = rc.readBroadcast(channelNo);
				byte signature = (byte) (rawMessage >> 24);
//				System.out.println("this sig: " + this.signature);
//				System.out.println("that sig: " + signature);
				if (this.signature == signature) {
					byte header = (byte) (rawMessage >> 16);
					short body = (short) rawMessage;
					return new Message(header, body);
				}
			}
			return null;
		} catch (Exception e) {
			return null;
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
