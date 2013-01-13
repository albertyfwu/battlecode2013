package base;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class HQRobot extends BaseRobot {
	
	public MapLocation[] encampmentJobs;
	public int numEncampmentsNeeded; // must be less than encampmentJobChannelList.length
	public MapLocation HQLocation;
	public MapLocation EnemyHQLocation;
	
	public HQRobot(RobotController rc) {
		super(rc);
		HQLocation = rc.getLocation();
		EnemyHQLocation = rc.senseEnemyHQLocation();
		numEncampmentsNeeded = Constants.INITIAL_NUM_ENCAMPMENTS_NEEDED; 
		
		MapLocation[] allEncampments = rc.senseAllEncampmentSquares();
		if (allEncampments.length < numEncampmentsNeeded) {
			numEncampmentsNeeded = allEncampments.length;
		}
		
		encampmentJobs = getClosestMapLocations(HQLocation, allEncampments, numEncampmentsNeeded);
	}

	@Override
	public void run() {
		try {
			
			// Spawn a soldier
			Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
			if (rc.canMove(dir)) {
				rc.spawn(dir);
			}
			
//			// TODO: find out what strategy to switch to?
//			// this.strategy = HQStrategy.xxx
//			if (rc.isActive()) {
//				switch (strategy) {
//				case CREATE_SOLDIER:
//					create_soldier();
//					break;
//				case RESEARCH_DEFUSION:
//					if (!rc.hasUpgrade(Upgrade.DEFUSION)) {
//						rc.researchUpgrade(Upgrade.DEFUSION);
//					}
//					break;
//				case RESEARCH_FUSION:
//					if (!rc.hasUpgrade(Upgrade.FUSION)) {
//						rc.researchUpgrade(Upgrade.FUSION);
//					}
//					break;
//				case RESEARCH_NUKE:
//					if (!rc.hasUpgrade(Upgrade.NUKE)) {
//						rc.researchUpgrade(Upgrade.NUKE);
//					}
//					break;
//				case RESEARCH_PICKAXE:
//					if (!rc.hasUpgrade(Upgrade.PICKAXE)) {
//						rc.researchUpgrade(Upgrade.PICKAXE);
//					}
//					break;
//				case RESEARCH_VISION:
//					if (!rc.hasUpgrade(Upgrade.VISION)) {
//						rc.researchUpgrade(Upgrade.VISION);
//					}
//					break;
//				default:
//					break;
//				}
//			}
		} catch (Exception e) {
			//
		}
	}
	
	public void create_soldier() throws GameActionException {
		// Spawn a soldier
		Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
		if (rc.canMove(dir)) {
			rc.spawn(dir);
		}
	}
	
	/**
	 * Finds array of closest MapLocations to rc.getLocation()
	 * in decreasing order
	 * 
	 * Specification: k must be <= allLoc.length
	 * Specification: array must not contain origin
	 * @param origin
	 * @param allLoc
	 * @param k
	 * @return
	 * 
	 * This still costs lots of bytecodes
	 */
	public static MapLocation[] getClosestMapLocations(MapLocation origin, MapLocation[] allLoc, int k) {
		MapLocation[] currentTopLocations = new MapLocation[k];
		
		int[] allDistances = new int[allLoc.length];
		for (int i=0; i<allLoc.length; i++) {
			allDistances[i] = origin.distanceSquaredTo(allLoc[i]);
		}
		
		int[] allLocIndex = new int[allLoc.length];
		
		int runningDist = 1000000;
		MapLocation runningLoc = null;
		int runningIndex = 0;
		for (int j = 0; j < k; j++) {
			runningDist = 1000000;
			for (int i=0; i<allLoc.length; i++) {
				if (allDistances[i] < runningDist && allLocIndex[i] == 0) {
					runningDist = allDistances[i];
					runningLoc = allLoc[i];
					runningIndex = i;
				}
			}
			currentTopLocations[j] = runningLoc;
			allLocIndex[runningIndex] = 1;
		}
		
		return currentTopLocations;
	}
	
//	public static void main(String[] arg0) {
//		MapLocation origin = new MapLocation(0,0);
//		
//		MapLocation test1 = new MapLocation(5,0);
//		MapLocation test2 = new MapLocation(4,1);
//		MapLocation test3 = new MapLocation(3,2);
//		MapLocation test4 = new MapLocation(1,3);
//		MapLocation test5 = new MapLocation(6,2);
//		MapLocation test6 = new MapLocation(0,5);
//		MapLocation test7 = new MapLocation(2,3);
//		
//		MapLocation[] allLoc = {test1, test2, test3, test4, test5, test6, test7};
//		
//		MapLocation[] results = getClosestMapLocations(origin, allLoc, 5);
//		
//		for (int i=0; i<results.length; i++) {
//			System.out.println("result " + i + ": " + results[i]);
//		}
//		
//	}
}
