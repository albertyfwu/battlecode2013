package base;

import battlecode.common.GameConstants;

public enum ChannelType {
	// test
	CHANNEL1,
	CHANNEL2,
	CHANNEL3,
	CHANNEL4,
	
	// for encampments
	CHANNELENC1,
	CHANNELENC2,
	CHANNELENC3,
	CHANNELENC4,
	CHANNELENC5,
	CHANNELENC6,
	CHANNELENC7,
	CHANNELENC8,
	
	// completion channels
	CHANNELCOMP1,
	CHANNELCOMP2,
	CHANNELCOMP3,
	CHANNELCOMP4,
	CHANNELCOMP5,
	CHANNELCOMP6,
	CHANNELCOMP7,
	CHANNELCOMP8;
	
	
	public static final int size = ChannelType.values().length;
	public static final int range = GameConstants.BROADCAST_MAX_CHANNELS / size;
}