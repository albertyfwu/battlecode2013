package basePlusSwarm;

import battlecode.common.GameConstants;

public enum ChannelType {

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
	COMP8,
	
	// broadcasting waypoints for clearing wide swath for army
	WAYPOINTS,
	
	// power level
	HQPOWERLEVEL,
	
	// checking if enemy nuke is half done
	ENEMY_NUKE_HALF_DONE;
	
	public static final int size = ChannelType.values().length;
	public static final int range = GameConstants.BROADCAST_MAX_CHANNELS / size;
}