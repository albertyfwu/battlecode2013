package base;

import java.util.ArrayList;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.Team;
import battlecode.common.Upgrade;

public class NavSystem {

	public static BaseRobot robot;
	public static RobotController rc;
	public static int[] directionOffsets;
	
	/**
	 * MUST CALL THIS METHOD BEFORE USING NavSystem
	 * @param myRC
	 */
	public static void init(BaseRobot myRobot) {
		robot = myRobot;
		rc = robot.rc;
		int robotID = rc.getRobot().getID();
		// Randomly assign soldiers priorities for trying to move left or right
		if (robotID % 4 == 0 || robotID % 4 == 1) {
			directionOffsets = new int[]{0,1,-1,2,-2};
		} else {
			directionOffsets = new int[]{0, -1,1,-2,2};
		}
	}
	
	/**
	 * Tells rc to go to a location while defusing mines along the way
	 * @param location
	 * @throws GameActionException
	 */
	public static void goToLocation(MapLocation location) throws GameActionException {
		Direction dir = rc.getLocation().directionTo(location);
		if (dir != Direction.OMNI) {
			goDirectionAndDefuse(dir);
		}
	}
	
	public static void goToLocationAvoidMines(MapLocation location) throws GameActionException {
		Direction dir = rc.getLocation().directionTo(location);
		if (dir != Direction.OMNI){
			goDirectionAvoidMines(dir);
		}
	}
	
	public static void goDirectionAndDefuse(Direction dir) throws GameActionException {
		Direction lookingAtCurrently = dir;
		lookAround: for (int d : directionOffsets) {
			lookingAtCurrently = Direction.values()[(dir.ordinal() + d + 8) % 8];
			if (rc.canMove(lookingAtCurrently)) {
				if (hasBadMine(rc.getLocation().add(lookingAtCurrently))) {
					rc.defuseMine(rc.getLocation().add(lookingAtCurrently));
				} else {
					rc.move(lookingAtCurrently);
				}
				break lookAround;
			}
		}
	}
	
	public static void goDirectionAvoidMines(Direction dir) throws GameActionException {
		Direction lookingAtCurrently = dir;
		boolean movedYet = false;
		lookAround: for (int d : directionOffsets) {
			lookingAtCurrently = Direction.values()[(dir.ordinal() + d + 8) % 8];
			if (rc.canMove(lookingAtCurrently)) {
				if (!hasBadMine(rc.getLocation().add(lookingAtCurrently))) {
					movedYet = true;
					rc.move(lookingAtCurrently);
				}
				break lookAround;
			}
			if (!movedYet) { // if the robot still hasn't moved
				if (rc.senseNearbyGameObjects(Robot.class, 2, rc.getTeam().opponent()).length == 0) {
					// if there are no nearby enemies
					rangedDefuseMine();
				}
			}
		}
	}
	
	public static void rangedDefuseMine() throws GameActionException {
		if (rc.hasUpgrade(Upgrade.DEFUSION)) {
			MapLocation[] mines = rc.senseMineLocations(rc.getLocation(), 14, rc.getTeam().opponent());
			if (mines.length > 0) {
				rc.defuseMine(mines[0]);
			}
		}
	}
	
	private static boolean hasBadMine(MapLocation location) {
		Team bombTeam = rc.senseMine(location);
		return !(bombTeam == null || bombTeam == rc.getTeam());
	}
	
	
	// HPA Pathfinding
	// Good reference: http://aigamedev.com/open/article/clearance-based-pathfinding/
	// TODO: Make it handle diagonals (to avoid defusing)
	public static ArrayList<MapLocation> calculatePath(RobotController rc, MapLocation start, MapLocation end) {
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
		if (maxDelta % TeamConstants.PATH_BOX_LEN == 0) {
			numBoxes = maxDelta / TeamConstants.PATH_BOX_LEN;
		} else {
			numBoxes = maxDelta / TeamConstants.PATH_BOX_LEN + 1;
		}
		int boxLen = numBoxes * TeamConstants.PATH_BOX_LEN;
		// Now that we have boxLen, find one corner of the HPA viewing box (big box)
		int bigX = (int)((x1 + x2) / 2f - boxLen / 2f);
		int bigY = (int)((y1 + y2) / 2f - boxLen / 2f);
		// Now, time to start our calculations:
		MapLocation midLocation = new MapLocation((x1+x2)/2, (y1+y2)/2);
		// Get all the mine locations around midLocation
		MapLocation[] mineLocations = rc.senseNonAlliedMineLocations(midLocation, (int)(2 * Math.pow(boxLen / 2, 2)));
		// TODO: Make this more efficient
		boolean[][] mineLocsSet = new boolean[GameConstants.MAP_MAX_WIDTH][GameConstants.MAP_MAX_HEIGHT];
		for (MapLocation mineLocation : mineLocations) {
			int tempx = mineLocation.x;
			int tempy = mineLocation.y;
			mineLocsSet[tempx][tempy] = true;
		}
		// TODO: Make this more efficient
		// Make an array that keeps track of mine counts
		int[][] mineCounts = new int[numBoxes][numBoxes]; // initialized to 0
		
		for (int i = 0; i < numBoxes; i++) {
			for (int j = 0; j < numBoxes; j++) {
				int i1 = bigX + i * TeamConstants.PATH_BOX_LEN;
				int j1 = bigY + j * TeamConstants.PATH_BOX_LEN;
				for (int k1 = 0; k1 < TeamConstants.PATH_BOX_LEN; k1++) {
					for (int k2 = 0; k2 < TeamConstants.PATH_BOX_LEN; k2++) {
						int i2 = i1 + k1;
						int j2 = j1 + k2;
						if (mineLocsSet[i2][j2]) { // there is a mine
							mineCounts[i][j]++;
						}
					}
				}
			}
		}
		
		boolean[][] visitedBoxes = new boolean[numBoxes][numBoxes];
		ArrayList<MapLocation> result = new ArrayList<MapLocation>();		
		int[] currentBox = coordsToBoxIndices(x1, y1, bigX, bigY, numBoxes);		
		int[] endBox = coordsToBoxIndices(x2, y2, bigX, bigY, numBoxes);
		result.add(boxToMapLocation(currentBox, bigX, bigY));
		visitedBoxes[currentBox[0]][currentBox[1]] = true;
		
		System.out.println("endBox: " + endBox[0] + ", " + endBox[1]);
		
		while (!areEqualBoxes(currentBox, endBox)) { // while we haven't arrived at the destination:
			int minHeuristic = Integer.MAX_VALUE;
			int[] minBox = null; // TODO: null check?
			for (int[] box : getNeighborBoxes(currentBox, numBoxes)) {
				if (!visitedBoxes[box[0]][box[1]]) {
					int mineCount = mineCounts[box[0]][box[1]];
					int currentHeuristic = heuristic(mineCount, box, endBox);					
					if (currentHeuristic < minHeuristic) {
						minHeuristic = currentHeuristic;
						minBox = box;
						visitedBoxes[box[0]][box[1]] = true;
					}
				}
			}
			currentBox = minBox;
			System.out.println("currentBox: " + currentBox[0] + ", " + currentBox[1]);
			result.add(boxToMapLocation(currentBox, bigX, bigY));
		}
		
		System.out.println("where are we now: " + Clock.getBytecodeNum());
		
		System.out.println(result.size());
		for (int i = 0; i < result.size(); i++) {
			MapLocation location = result.get(i);
			System.out.println(location.x + ", " + location.y);
		}
			
		System.out.println("bye");
		
		return result;
	}
	
	// TODO: Refine; fix termination condition
	
	// Heuristic for moving will be:
	// (Distance to end) + (mineCount / TeamConstants.HPA_BOX_LEN * GameConstants.MINE_DEFUSE_DELAY)
	// TODO: Choose MINE_DEFUSE_DELAY OR MINE_DEFUSE_DEFUSION_DELAY correctly
	// TODO: Also, don't let it move backwards? Make distance even more imperative.
	// TODO: make a better scoring function, and make sure the path search terminates
	public static int heuristic(int mineCount, int[] startBox, int[] endBox) {
		// if a box is TeamConstants.PATH_BOX_LEN away, and has the maximum TeamConstants.PATH_BOX_LEN^2 fewer bombs, we should still prefer
		// moving towards the destination if we're close enough
		return (int)(15*GameConstants.MINE_DEFUSE_DELAY * Math.pow(manhattanDistance(startBox, endBox), 0.25)) + mineCount * GameConstants.MINE_DEFUSE_DELAY / TeamConstants.PATH_BOX_LEN;
//		return (int)Math.pow(distance(startBox, endBox), 2) + mineCount * GameConstants.MINE_DEFUSE_DELAY / TeamConstants.PATH_BOX_LEN;
	}
	
	public static int manhattanDistance(int[] startBox, int[] endBox) {
		return Math.abs(endBox[0] - startBox[0]) + Math.abs(endBox[1] - startBox[1]);
	}
	
	public static int distance(int[] startBox, int[] endBox) {
		return (int)(Math.sqrt(Math.pow(startBox[0]-endBox[0], 2) + Math.pow(startBox[1]-endBox[1], 2)));
	}
	
	public static int[] coordsToBoxIndices(int x, int y, int bigX, int bigY, int numBoxes) {
		int normX = x - bigX;
		int normY = y - bigY;
		int resultX = normX / TeamConstants.PATH_BOX_LEN;
		int resultY = normY / TeamConstants.PATH_BOX_LEN;
		// TODO: Fix this hack
		if (resultX == numBoxes) {
			resultX = numBoxes - 1;
		}
		if (resultY == numBoxes) {
			resultY = numBoxes - 1;
		}
		return new int[] {resultX, resultY};
	}
	
	public static MapLocation boxIndicesToMapLocation(int i, int j, int bigX, int bigY) {
		int resultX = bigX + i * TeamConstants.PATH_BOX_LEN + TeamConstants.PATH_BOX_LEN / 2;
		int resultY = bigY + j * TeamConstants.PATH_BOX_LEN + TeamConstants.PATH_BOX_LEN / 2;
		return new MapLocation(resultX, resultY);
	}
	
	public static MapLocation boxToMapLocation(int[] box, int bigX, int bigY) {
		return boxIndicesToMapLocation(box[0], box[1], bigX, bigY);
	}
	
	public static boolean areEqualBoxes(int i1, int j1, int i2, int j2) {
		return (i1 == i2 && j1 == j2);
	}
	
	// Ensure that these are length-2 int arrays (coordinates)
	public static boolean areEqualBoxes(int[] box1, int[] box2) {
		return areEqualBoxes(box1[0], box1[1], box2[0], box2[1]);
	}
	
	// TODO: Change later; also add in diagonals
	public static ArrayList<int[]> getNeighborBoxes(int[] box, int numBoxes) {
		ArrayList<int[]> result = new ArrayList<int[]>();
		int i = box[0];
		int j = box[1];
		if (i > 0) { // left neighbor
			result.add(new int[] {i-1, j});
		}
		if (j > 0) { // bottom
			result.add(new int[] {i, j-1});			
		}
		if (i < numBoxes - 1) { // right
			result.add(new int[] {i+1, j});
		}
		if (j < numBoxes - 1) { // top
			result.add(new int[] {i, j+1});
		}
		return result;
	}
}