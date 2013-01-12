package base;

public interface TeamConstants {
	
	/**
	 * Broadcasting
	 */
	// number of redundant channels we use for communication
	public static final int REDUNDANT_CHANNELS = 1;
	
	// how frequently we change the channels we use for broadcasting
	// var = n means channel will cycle every n turns
	public static final int CHANNEL_CYCLE = 10;
	
	/**
	 * Smart Waypoints
	 */
	
	// How far off we should look each time we need to calculate a new waypoint
	public static final int PATH_OFFSET_RADIUS = 3;
	
	// How large of a circle we should be checking each time we calculate a new waypoint
	public static final int PATH_CHECK_RADIUS = 3;
	
	// The squared radius distance at which we stop over-valuing mines, and just go straight for the endLocation
	public static final int PATH_GO_ALL_IN_SQ_RADIUS = 64;
}