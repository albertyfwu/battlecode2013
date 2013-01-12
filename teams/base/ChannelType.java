package base;

import battlecode.common.GameConstants;

public enum ChannelType {
	// test
	CHANNEL1,
	CHANNEL2,
	CHANNEL3,
	CHANNEL4;
	
	public static final int size = ChannelType.values().length;
	public static final int range = GameConstants.BROADCAST_MAX_CHANNELS / size;
}