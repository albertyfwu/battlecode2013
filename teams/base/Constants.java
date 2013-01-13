package base;

public interface Constants {
	
	/**
	 * Broadcasting
	 */
	// number of redundant channels we use for communication
	public static final int REDUNDANT_CHANNELS = 2;
	
	// how frequently we change the channels we use for broadcasting
	// var = n means channel will cycle every n turns
	public static final int CHANNEL_CYCLE = 10;
	
	/**
	 * All Waypoints
	 */
	
	// The squared distance away from a waypoint at which we can safely go onto the next waypoint
	public static final int WAYPOINT_SQUARED_DISTANCE_CHECK = 2;
	
	/**
	 * Smart Waypoints
	 */
	
	// How far off we should look each time we need to calculate a new waypoint
	public static final int PATH_OFFSET_RADIUS = 3;
	
	// How large of a circle we should be checking each time we calculate a new waypoint
	public static final int PATH_CHECK_RADIUS = 3;
	
	// The squared radius distance at which we stop over-valuing mines, and just go straight for the endLocation
	public static final int PATH_GO_ALL_IN_SQ_RADIUS = 64;
	
	/**
	 * Backdoor Waypoints
	 */
	
	// How close we should be to the wall while doing a backdoor navigation
	public static final int BACKDOOR_WALL_BUFFER = 1;
	
	/**
	 * Encampments
	 */
	public static final int INITIAL_NUM_ENCAMPMENTS_NEEDED = 3;
}