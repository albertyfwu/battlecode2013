package base;

public interface TeamConstants {
	
	/**
	 * Broadcasting
	 */
	// number of redundant channels we use for communication
	public static final int REDUNDANT_CHANNELS = 3;
	
	// how frequently we change the channels we use for broadcasting
	// higher is more frequent
	public static final int CHANNEL_CYCLE_FREQ = 10;
	
	/**
	 * HPA Pathfinding
	 */
	// length of an HPA box (height or width)
	public static final int HPA_BOX_LEN = 5;
}