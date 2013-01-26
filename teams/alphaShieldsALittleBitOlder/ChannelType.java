package alphaShieldsALittleBitOlder;

import battlecode.common.GameConstants;

public enum ChannelType {

	// for strategy
	STRATEGY,
	
	// for reporting artillery seen
	ARTILLERY_SEEN,
	
	// channel for telling a designated encampment to commit suicide
	ENCAMPMENT_SUICIDE,
	
	// for encampments
	ENC1,
	ENC2,
	ENC3,
	ENC4,
	ENC5,
	
	// completion channels
	COMP1,
	COMP2,
	COMP3,
	COMP4,
	COMP5,
	
	// broadcasting waypoints for clearing wide swath for army
	WAYPOINTS,
	
	// power level
	HQPOWERLEVEL,
	
	// checking if enemy nuke is half done
	ENEMY_NUKE_HALF_DONE,
	
	// check if our nuke is half done
	OUR_NUKE_HALF_DONE;
	
	public static final int size = ChannelType.values().length;
	public static final int range = GameConstants.BROADCAST_MAX_CHANNELS / size;
}