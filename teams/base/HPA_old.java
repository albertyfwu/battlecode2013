package base;

import java.util.HashSet;

import battlecode.common.Clock;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class HPA_old {
	
	public static MapLocation[][] vertDoors;
	public static MapLocation[][] horzDoors;
	
	// default constructor
	public HPA_old() {
	}
	
	// HPA Pathfinding
	// Good reference: http://aigamedev.com/open/article/clearance-based-pathfinding/
	// TODO: Make it handle diagonals (to avoid defusing)
	public static void calculatePath(RobotController rc, MapLocation start, MapLocation end) {
		// Turn start and end points into (x1, y1) and (x2, y2)
		int x1 = start.x;
		int y1 = start.y;
		int x2 = end.x;
		int y2 = end.y;
		// Calculate the deltas for x and y distances
		int dX = x2 - x1;
		int dY = y2 - y1;		
		int maxDelta = Math.max(Math.abs(dX), Math.abs(dY));
		// Find the number of boxes this corresponds to (based on box length of
		// TeamConstants.HPA_BOX_LEN)
		int numBoxes = 0;
		if (maxDelta % TeamConstants.HPA_BOX_LEN == 0) {
			numBoxes = maxDelta / TeamConstants.HPA_BOX_LEN;
		} else {
			numBoxes = maxDelta / TeamConstants.HPA_BOX_LEN + 1;
		}
		int boxLen = numBoxes * TeamConstants.HPA_BOX_LEN;
		// Now that we have boxLen, find one corner of the HPA viewing box (big box)
		int bigX = (int)((x1 + x2) / 2f - boxLen / 2f);
		int bigY = (int)((y1 + y2) / 2f - boxLen / 2f);
		// Now, time to start our calculations:
		/* v[][] stands for vertDoors[][]
		 * h[][] stands for horzDoors[][]
		 * (bigX, bigY) are the coordinates of the bottom-left corner
		 * 
		 *  - - - - - - - - -
		 * |     |     |     |
		 * |    v02   v12    |
		 * |     |     |     |
		 *  -h01- -h11- -h21-
		 * |     |     |     |
		 * |    v01   v11    |
		 * |     |     |     |
		 *  -h00- -h10- -h20-
		 * |     |     |     |
		 * |    v00   v10    |
		 * |     |     |     |
		 *  - - - - - - - - -
		 */
		MapLocation midLocation = new MapLocation((x1+x2)/2, (y1+y2)/2);
//		System.out.println("midLocationx: " + midLocation.x);
//		System.out.println("midLocationy: " + midLocation.y);
		// Get all the mine locations:
		MapLocation[] mineLocations = rc.senseNonAlliedMineLocations(midLocation, (int)(2 * Math.pow(boxLen / 2, 2)));
//		System.out.println("radiusSquared: " + (int)(2 * Math.pow(boxLen / 2, 2)));
		// TODO: Make this more efficient
		boolean[][] mineLocsSet = new boolean[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];
		for (MapLocation mineLocation : mineLocations) {
			int tempx = mineLocation.x;
			int tempy = mineLocation.y;
			mineLocsSet[tempx][tempy] = true;
		}
		vertDoors = new MapLocation[numBoxes][numBoxes]; // holds coords of good doors
		horzDoors = new MapLocation[numBoxes][numBoxes]; // holds coords of good doors
		for (int i = 0; i < numBoxes; i++) {
			for (int j = 0; j < numBoxes; j++) {
				// Use better heuristics: choose horzDoors and vertDoors
				// that are along the straight line from start to end
				// horzDoors
				int currentBestX = 0;
				int currentBestCountX = Integer.MAX_VALUE;
				for (int k = 0; k < TeamConstants.HPA_BOX_LEN; k++) {
					int count = 0;
					for (int n = -TeamConstants.HPA_CHECK_WIDTH; n <= TeamConstants.HPA_CHECK_WIDTH; n++) {
						int tempx = bigX + i * TeamConstants.HPA_BOX_LEN + k;
						int tempy = bigY + (j + 1) * TeamConstants.HPA_BOX_LEN;
						if (mineLocsSet[tempx][tempy]) {
							count++;
						}
					}
					if (count < currentBestCountX) {
						currentBestX = k;
						currentBestCountX = count;
					}
				}
				horzDoors[i][j] = new MapLocation(bigX + i * TeamConstants.HPA_BOX_LEN + currentBestX,
						bigY + (j + 1) * TeamConstants.HPA_BOX_LEN);
				// vertDoors
				int currentBestY = 0;
				int currentBestCountY = Integer.MAX_VALUE;
				for (int k = 0; k < TeamConstants.HPA_BOX_LEN; k++) {
					int count = 0;
					for (int n = -TeamConstants.HPA_CHECK_WIDTH; n <= TeamConstants.HPA_CHECK_WIDTH; n++) {
						int tempx = bigX + (i + 1) * TeamConstants.HPA_BOX_LEN;
						int tempy = bigY + j * TeamConstants.HPA_BOX_LEN + k;
						if (mineLocsSet[tempx][tempy]) {
							count++;
						}
					}
					if (count < currentBestCountY) {
						currentBestY = k;
						currentBestCountY = count;
					}
				}
				vertDoors[i][j] = new MapLocation(bigX + (i + 1) * TeamConstants.HPA_BOX_LEN,
						bigY + j * TeamConstants.HPA_BOX_LEN + currentBestY);
			}
		}
		// Now we're done with horzDoors and vertDoors
		// Now iterate through the mineLocsSet to compute number of mines.
		// matrix of edges
		int[][] adjMatrix = new int[2 * numBoxes * numBoxes][2 * numBoxes * numBoxes];
		for (int i = 0; i < numBoxes; i++) {
			for (int j = 0 ; j < numBoxes; j++) {
				// Make horzDoors map to 0 - numBoxes^2
				// Make vertDoors map to numBoxes^2 - 2 * numBoxes
				adjMatrix[horzDoorsToIndex(i, j, numBoxes)][vertDoorsToIndex(i, j, numBoxes)] = 1;
				if (j > 0) {
					adjMatrix[horzDoorsToIndex(i, j, numBoxes)][horzDoorsToIndex(i, j-1, numBoxes)] = 1;
					adjMatrix[vertDoorsToIndex(i, j, numBoxes)][horzDoorsToIndex(i, j-1, numBoxes)] = 1;
				}
				if (i > 0) {
					adjMatrix[horzDoorsToIndex(i, j, numBoxes)][vertDoorsToIndex(i-1, j, numBoxes)] = 1;
					adjMatrix[vertDoorsToIndex(i, j, numBoxes)][vertDoorsToIndex(i-1, j, numBoxes)] = 1;
				}
				if (i > 0 && j > 0) {
					adjMatrix[horzDoorsToIndex(i, j-1, numBoxes)][vertDoorsToIndex(i-1, j, numBoxes)] = 1;
				}
			}
		}
		
		// Try to find some reasonably optimal paths between these doors to the final destination
//		System.out.println("hi there: " + Clock.getBytecodeNum());
	}
	
	public static int horzDoorsToIndex(int x, int y, int numBoxes) {
		return x * numBoxes + y;
	}
	
	public static int vertDoorsToIndex(int x, int y, int numBoxes) {
		return numBoxes * numBoxes + x * numBoxes + y;
	}
}