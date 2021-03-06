package hardcoreBot;

import battlecode.common.GameConstants;

public enum ChannelType {
	// test
	CHANNEL1,
	CHANNEL2,
	CHANNEL3,
	CHANNEL4,
	
	// for encampments
	ENC1,
	ENC2,
	ENC3,
	ENC4,
	ENC5,
	ENC6,
	ENC7,
	ENC8,
	
	// completion channels
	COMP1,
	COMP2,
	COMP3,
	COMP4,
	COMP5,
	COMP6,
	COMP7,
	COMP8;	
	
	public static final int size = ChannelType.values().length;
	public static final int range = GameConstants.BROADCAST_MAX_CHANNELS / size;
}