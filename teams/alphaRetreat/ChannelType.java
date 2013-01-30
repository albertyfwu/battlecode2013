package alphaRetreat;

import battlecode.common.GameConstants;

public enum ChannelType {

	// for broadcasting move-out
	MOVE_OUT,
	
	// for generator count
	GEN_COUNT,
	
	// for reporting artillery seen
	ARTILLERY_SEEN,
	
	// for encampments
	ENC1,
	ENC2,
	ENC3,
	ENC4,
	ENC5,
	ENCSHIELD,
	
	// completion channels
	COMP1,
	COMP2,
	COMP3,
	COMP4,
	COMP5,
	COMPSHIELD,
	
	// power level
	HQPOWERLEVEL,
	
	// checking if enemy nuke is half done
	ENEMY_NUKE_HALF_DONE,
	
	// retreat channel
	RETREAT_CHANNEL,
	
	//shield location
	SHIELD_LOCATION;
	
	public static final int size = ChannelType.values().length;
	public static final int range = GameConstants.BROADCAST_MAX_CHANNELS / size;
}