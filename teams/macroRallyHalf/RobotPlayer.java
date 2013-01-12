package macroRallyHalf;

import macroRallyHalf.BroadcastChannel;
import macroRallyHalf.BroadcastSystem;
import macroRallyHalf.ChannelType;
import macroRallyHalf.Message;
import battlecode.common.*;

public class RobotPlayer{
	
	private static RobotController rc;
	private static MapLocation rallyPoint = null;
	private static int robotID = 0;
	private static int[] directionOffsets;
	private static boolean enemyHasMines = false;
	
	private static int designatedCapturer = 0;
	private static int capturerWaitCounter = 0;
	private static boolean isCapturer = false;
	private static MapLocation nextEncampment = null;
	
	private static int currentCaptureToHQ = 0;
	
	private static int numEncampments = 0; 
	
	private static MapLocation enemyHQLocation = null;
	private static MapLocation HQLocation = null;
	private static int rushDistanceSquared = 0;
	
	private static int numTotalEncampments = 10000;
	
	private static double ratioSuppliers = 0.8;
	
//	private static int channelNumEncampments = 7000;
//	private static int channelNumTotalEncampments = 7001;
//	private static int channelCaptureToHQ = 1339;
//	private static int channelHQToCapture = 4339;
//	private static int channelNearestEnc = 3001;
	
	public static void run(RobotController myRC){
		rc = myRC;
		robotID = rc.getRobot().getID();
		if (robotID % 4 == 0 || robotID % 4 == 1) {
			directionOffsets = new int[]{0,1,-1,2,-2};
		} else {
			directionOffsets = new int[]{0,-1,1,-2,2};
		}
		HQLocation = rc.senseHQLocation();
		enemyHQLocation = rc.senseEnemyHQLocation();
		rushDistanceSquared = rc.senseHQLocation().distanceSquaredTo(rc.senseEnemyHQLocation());

		while(true){
			try{
				BroadcastChannel channelHQToCapture = BroadcastSystem.getChannelByType(ChannelType.CHANNEL2);
				int captureID = 0;
				Message message = channelHQToCapture.read(rc);
				if (message != null) {
					captureID = message.body;
				}
				
				
				if (rc.getRobot().getID() == captureID) {
					isCapturer = true;
				}
				
				if (rc.getType()==RobotType.SOLDIER && isCapturer == false){
					if (rc.isActive()) {
//						if (rallyPoint == null) {
//						int hashedLoc = rc.readBroadcast(3001);
//						if (hashedLoc != 0) {
//							int y = hashedLoc % 1000;
//							int x = (hashedLoc - y)/1000;
//							rallyPoint = new MapLocation(x,y);
//						}
//					}
					
						Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,100000,rc.getTeam().opponent());
						if(enemyRobots.length==0 || (enemyRobots.length == 1 && rc.senseRobotInfo(enemyRobots[0]).type == RobotType.HQ)){//no enemies nearby
							Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class, 1000000, rc.getTeam());
							
							BroadcastChannel channelNumEncampments = BroadcastSystem.getChannelByType(ChannelType.CHANNEL3);
							Message message1 = channelNumEncampments.read(rc);
							if (message1 != null) {
								numEncampments = message1.body;
							}
							
							BroadcastChannel channelNumTotalEncampments = BroadcastSystem.getChannelByType(ChannelType.CHANNEL5);
							Message message2 = channelNumTotalEncampments.read(rc);
							if (message2 != null) {
								numTotalEncampments = message2.body;
							}
														//						if (rc.senseEncampmentSquare(rc.getLocation())){
							//							rc.captureEncampment(RobotType.SUPPLIER);
							//						}
							boolean layedMine = false;
							if (alliedRobots.length - numEncampments < 25){ // if < 25 allied robots
								if (rc.senseMine(rc.getLocation()) == null) {
									if (Math.random() < 0.1) {
										layedMine = true;
										rc.layMine();
									}
								}
								if (!layedMine) {
									float ratio = (float) ((1.0 * numEncampments)/(1.0 * numTotalEncampments));
									rallyPoint = findRallyPoint(ratio);
									goToLocation(rallyPoint);
								}

							}else{
								goToLocation(enemyHQLocation);
							}
						} else {//someone spotted
							int[] closestEnemyInfo = getClosestRobot(enemyRobots);
							int closestDist = closestEnemyInfo[0];
							MapLocation closestEnemy = new MapLocation(closestEnemyInfo[1], closestEnemyInfo[2]);

							Robot[] nearbyAllies= rc.senseNearbyGameObjects(Robot.class,64,rc.getTeam());
							if (nearbyAllies.length > 3 * enemyRobots.length) { // if we outnumber them by a lot
								if (closestDist > 4) {
									if (rc.hasUpgrade(Upgrade.DEFUSION) && Math.random() < 0.05) { // defuse randomly
										MapLocation[] mines = rc.senseMineLocations(rc.getLocation(), 14, rc.getTeam().opponent());
										if (mines.length > 0) {
											rc.defuseMine(mines[0]);
										}
									} else { // if not defusing
										goToLocation(closestEnemy);
									}
								}
							} else {
								goToLocationAvoidMines(closestEnemy);
							}
						}
					}

				} else if (rc.getType()==RobotType.SOLDIER && isCapturer == true) {
					captureCode();
				} else if (rc.getType()==RobotType.HQ){ // if HQ
					hqCode();
				} 
			
			}catch (Exception e){
				System.out.println("caught exception before it killed us:");
				System.out.println(rc.getRobot().getID());
				e.printStackTrace();
			}
			rc.yield();
		}
	}
	
	private static void goToLocation(MapLocation whereToGo) throws GameActionException {

		int dist = rc.getLocation().distanceSquaredTo(whereToGo);
		if (dist>0){
			Direction dir = rc.getLocation().directionTo(whereToGo);
			goDirectionAndDefuse(dir);
		}
	}
	
	private static void goToLocationAvoidMines(MapLocation whereToGo) throws GameActionException {

		int dist = rc.getLocation().distanceSquaredTo(whereToGo);
		if (dist>0){
			Direction dir = rc.getLocation().directionTo(whereToGo);
			goDirectionAvoidMines(dir);
		}
	}
	
	private static void goDirectionAndDefuse(Direction dir) throws GameActionException {
		Direction lookingAtCurrently = dir;
		lookAround: for (int d:directionOffsets){
			lookingAtCurrently = Direction.values()[(dir.ordinal()+d+8)%8];
			if(rc.canMove(lookingAtCurrently)){
				if (badBomb(rc.getLocation().add(lookingAtCurrently))) {
					rc.defuseMine(rc.getLocation().add(lookingAtCurrently));
				} else {
					rc.move(lookingAtCurrently);
					rc.setIndicatorString(0, "Last direction moved: "+lookingAtCurrently.toString());
				}
				break lookAround;
			}
		}
	}
	
	private static void goDirectionAvoidMines(Direction dir) throws GameActionException {
		Direction lookingAtCurrently = dir;
		boolean movedYet = false;
		lookAround: for (int d:directionOffsets){
			lookingAtCurrently = Direction.values()[(dir.ordinal()+d+8)%8];
			if(rc.canMove(lookingAtCurrently)){
				if (!badBomb(rc.getLocation().add(lookingAtCurrently))) {
					movedYet = true;
					rc.move(lookingAtCurrently);
					rc.setIndicatorString(0, "Last direction moved: "+lookingAtCurrently.toString());
				}
				break lookAround;
			}
			if (movedYet == false) { // if it still hasn't moved
				if (rc.senseNearbyGameObjects(Robot.class, 2, rc.getTeam().opponent()).length == 0) {
					//if not getting shot at
					rangedDefuseMine();
				}
				
			}
		}
	}
	
	private static void rangedDefuseMine() throws GameActionException {
		
		if (rc.hasUpgrade(Upgrade.DEFUSION)) {
			MapLocation[] mines = rc.senseMineLocations(rc.getLocation(), 14, rc.getTeam().opponent());
			if (mines.length > 0) {
				rc.defuseMine(mines[0]);
			}
		}
	}

	private static MapLocation findRallyPoint(float ratio) {
		MapLocation enemyLoc = enemyHQLocation;
		MapLocation ourLoc = rc.senseHQLocation();
		int x, y;
		if (ratio < 0.2) {
			x = (enemyLoc.x+4*ourLoc.x)/5;
			y = (enemyLoc.y+4*ourLoc.y)/5;
		} else {
			x = (int) (enemyLoc.x * ratio + (1-ratio) * ourLoc.x);
			y = (int) (enemyLoc.y * ratio + (1-ratio) * ourLoc.y);
		}
		MapLocation rallyPoint = new MapLocation(x,y);
		return rallyPoint;
	}
	
	public static void captureCode() throws GameActionException {
		currentCaptureToHQ += 1;

		BroadcastChannel channelCaptureToHQ = BroadcastSystem.getChannelByType(ChannelType.CHANNEL1);
		channelCaptureToHQ.write(rc, new Message(0, (short)currentCaptureToHQ));

		if (rc.isActive()) {

			if (nextEncampment == null) {
				BroadcastChannel channelNearestEnc = BroadcastSystem.getChannelByType(ChannelType.CHANNEL4);
				int hashedLoc = 0;
				Message message = channelNearestEnc.read(rc);
				if (message != null) {
					hashedLoc = message.body;
				}

				if (hashedLoc != 0) {
					int y = (byte) hashedLoc;
					int x = (byte)(hashedLoc >> 8);
					nextEncampment = new MapLocation(x,y);
				}
			}
			if (rc.senseEncampmentSquare(rc.getLocation())) {
				if (Math.random() < ratioSuppliers) {
					rc.captureEncampment(RobotType.SUPPLIER);
				} else {
					rc.captureEncampment(RobotType.GENERATOR);
				}

			} else {
				goToLocation(nextEncampment);
			}
		}

	}

	public static void hqCode() throws GameActionException{
		
		
//		if (Clock.getRoundNum() > 2000) { // for testing purposes
//			rc.resign();
//		} 
		// Broadcast nearest encampment
		MapLocation[] neutralEncampments = rc.senseEncampmentSquares(rc.getLocation(), 1000000,Team.NEUTRAL);
		if (numTotalEncampments == 10000) {
			numTotalEncampments = neutralEncampments.length;
		}
		
		MapLocation closestEnc = getClosestEncampemnt(neutralEncampments);
		
		BroadcastChannel channelClosestEnc = BroadcastSystem.getChannelByType(ChannelType.CHANNEL4);
		if (closestEnc == null) {
			channelClosestEnc.write(rc, new Message(0, 0)); // broadcast closestEnc location
		} else {
			channelClosestEnc.write(rc, new Message(0, (closestEnc.x << 8) + closestEnc.y)); // broadcast closestEnc location
		}
		
		Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class,100000,rc.getTeam());
		
		
		MapLocation[] alliedEncampments = rc.senseEncampmentSquares(rc.getLocation(), 1000000, rc.getTeam());
		numEncampments = alliedEncampments.length;
		
		BroadcastChannel channelNumEncampments = BroadcastSystem.getChannelByType(ChannelType.CHANNEL3);
		channelNumEncampments.write(rc, new Message(0, numEncampments));

		BroadcastChannel channelNumTotalEncampments = BroadcastSystem.getChannelByType(ChannelType.CHANNEL5);
		channelNumTotalEncampments.write(rc, new Message(0, numTotalEncampments));
		
		int numAlliedSoldiers = alliedRobots.length - alliedEncampments.length;
		if (numAlliedSoldiers > 5) {
//			if (Clock.getRoundNum() > 800 && Clock.getRoundNum() < 1000) {
//				System.out.println("designatedCapturer: " + designatedCapturer);
//			}
			if (designatedCapturer == 0) {
				int[] closeRobotInfo = getClosestSoldier(alliedRobots);
				designatedCapturer = closeRobotInfo[3];		
			} else {
				
				BroadcastChannel channelCaptureToHQ = BroadcastSystem.getChannelByType(ChannelType.CHANNEL1);
				
				Message message = channelCaptureToHQ.read(rc);
				if (message!= null && message.body == currentCaptureToHQ) {
					capturerWaitCounter++;
				} else {
					if (message != null) {
						currentCaptureToHQ = message.body;
					}
					capturerWaitCounter = 0;
				}
				
				if (capturerWaitCounter == 5) { // if you don't hear back from him
					designatedCapturer = 0; // reset
				}
			}
			BroadcastChannel channelHQToCapture = BroadcastSystem.getChannelByType(ChannelType.CHANNEL2);
			channelHQToCapture.write(rc, new Message(9, designatedCapturer));		
		}	

		
		if (rc.isActive()) {
			if (!rc.hasUpgrade(Upgrade.DEFUSION) && rc.senseEnemyNukeHalfDone() && numAlliedSoldiers > 20) {
				rc.researchUpgrade(Upgrade.DEFUSION);
			} else if (numAlliedSoldiers > 25 && Clock.getRoundNum() > 500) {
				if (!rc.hasUpgrade(Upgrade.DEFUSION)) {
					rc.researchUpgrade(Upgrade.DEFUSION);
				} else if (!rc.hasUpgrade(Upgrade.PICKAXE)) {
					rc.researchUpgrade(Upgrade.PICKAXE);
				} else if (!rc.hasUpgrade(Upgrade.FUSION)) {
					rc.researchUpgrade(Upgrade.FUSION);
				}
			} else {
				// Spawn a soldier
				Direction desiredDir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
				Direction dir = getSpawnDirection(rc, desiredDir);
				if (dir != null) {
					rc.spawn(dir);
				}
			}
		}
	}
	
	/**
	 * helper fcn to compute if location contains a bad bomb
	 * @param rc
	 * @param loc
	 * @return
	 */
	private static boolean badBomb(MapLocation loc) {
		Team isBomb = rc.senseMine(loc);
		if (isBomb == null || isBomb == rc.getTeam()) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * helper fcn to help compute nearest enemy robot
	 * @param enemyRobots - must not be empty
	 * @return
	 * @throws GameActionException 
	 */
	private static int[] getClosestRobot(Robot[] enemyRobots) throws GameActionException {
		int closestDist = 100000;
		MapLocation closestEnemy=null;
		int robotID = 0;
		for (int i=0;i<enemyRobots.length;i++){
			Robot arobot = enemyRobots[i];
			RobotInfo arobotInfo = rc.senseRobotInfo(arobot);
			int dist = arobotInfo.location.distanceSquaredTo(rc.getLocation());
			if (dist<closestDist){
				closestDist = dist;
				closestEnemy = arobotInfo.location;
				robotID = arobotInfo.robot.getID();
			}
		}
		int[] output = new int[4];
		output[0] = closestDist;
		output[1] = closestEnemy.x;
		output[2] = closestEnemy.y;
		output[3] = robotID;
		
		return output;
	}
	
	/**
	 * helper fcn to help compute nearest enemy robot
	 * @param enemyRobots - must not be empty
	 * @return
	 * @throws GameActionException 
	 */
	private static int[] getClosestSoldier(Robot[] robots) throws GameActionException {
		int closestDist = 100000;
		MapLocation closestSoldier=null;
		int robotID = 0;
		for (int i=0;i<robots.length;i++){
			Robot arobot = robots[i];
			RobotInfo arobotInfo = rc.senseRobotInfo(arobot);
			int dist = arobotInfo.location.distanceSquaredTo(rc.getLocation());
			if (dist<closestDist && arobotInfo.type == RobotType.SOLDIER){
				closestDist = dist;
				closestSoldier = arobotInfo.location;
				robotID = arobotInfo.robot.getID();
			}
		}
		int[] output = new int[4];
		if (closestSoldier != null) {
			output[0] = closestDist;
			output[1] = closestSoldier.x;
			output[2] = closestSoldier.y;
			output[3] = robotID;
		} else {
			return null;
		}
		
		
		return output;
	}
	
	/**
	 * helper fcn to see what direction to actually go given a desired direction
	 * @param rc
	 * @param dir
	 * @return
	 */
	private static Direction getSpawnDirection(RobotController rc, Direction dir) {
		if (rc.canMove(dir)) {
			return dir;
		} else if (rc.canMove(dir.rotateLeft())) {
			return dir.rotateLeft();
		} else if (rc.canMove(dir.rotateRight())) {
			return dir.rotateRight();
		} else if (rc.canMove(dir.rotateLeft().rotateLeft())) {
			return dir.rotateLeft().rotateLeft();
		} else if (rc.canMove(dir.rotateRight().rotateRight())) {
			return dir.rotateRight().rotateRight();
		} else if (rc.canMove(dir.rotateLeft().opposite())) {
			return dir.rotateLeft().opposite();
		} else if (rc.canMove(dir.rotateRight().opposite())) {
			return dir.rotateRight().opposite();
		} else {
			return dir.opposite();
		}
	}
	
	/**
	 * finds closest encampment
	 * @param encampments
	 * @return
	 */
	private static MapLocation getClosestEncampemnt(MapLocation[] encampments) {
		int closestDist = 100000;
		MapLocation closestEnc = null;
		for (MapLocation e:encampments){
			int dist = rc.getLocation().distanceSquaredTo(e);
			if (dist<closestDist){
				closestDist = dist;
				closestEnc = e;
			}
		}
		
		return closestEnc;
	}
	
	private static int robotIdHash(int id) {
		return id;
	}
	
	public static int[] locToIndex(MapLocation ref, MapLocation test,int offset){
		int[] index = new int[2];
		index[0] = test.y-ref.y+offset;
		index[1] = test.x-ref.x+offset;
		return index;
	}
	
	public static int[][] populate5by5board() throws GameActionException{
		
		MapLocation myLoc=rc.getLocation();
		int[][] array = new int[5][5];

		Robot[] nearbyRobots = rc.senseNearbyGameObjects(Robot.class,8);
//		rc.setIndicatorString(2, "number of bots: "+nearbyRobots.length);
		for (Robot aRobot:nearbyRobots){
			RobotInfo info = rc.senseRobotInfo(aRobot);
			int[] index = locToIndex(myLoc,info.location,2);
			if(index[0]>=0&&index[0]<=4&&index[1]>=0&&index[1]<=4){
				if (info.type != RobotType.SOLDIER) {
					array[index[0]][index[1]]=100;
				}
			}
		}
		return array;
	}
	
	public static int[] runBFS(int[][] encArray, int goalx, int goaly) {
		int[][] distanceArray = new int[encArray.length][encArray[0].length];
		distanceArray[2][2] = 1;
		for (int y = 0; y<5; y++) {
			for (int x=0; x<5; x++) {
				if (encArray[y][x] > 0) {
					distanceArray[y][x] = -1;
				}
			}
		}
		
		int currValue = 1;
		
		whileLoop: while(currValue < 25) {			
			for (int y = 0; y<5; y++) {
				for (int x=0; x<5; x++) {
					if (distanceArray[y][x] == currValue) {
						if (y == goaly && x == goalx) {
							break whileLoop;
						} else {
							propagate(distanceArray, x, y, currValue + 1);
						}
					}
				}
			}
			currValue++;
		}
		
		int shortestDist = distanceArray[goaly][goalx] - 1;
		int[] output = new int[shortestDist];
		currValue = shortestDist;
		int currx = goalx;
		int curry = goaly;
		int turn = 0;
		
		while(currValue > 1) {
			int[][] neighbors = getNeighbors(currx, curry);
			forloop: for (int[] neighbor: neighbors) {
				int nx = neighbor[0];
				int ny = neighbor[1];
				if (ny < 5 && ny >= 0 && nx < 5 && nx >= 0) {
					if (distanceArray[ny][nx] == currValue) {
						turn = computeTurn(currx, curry, nx, ny);						
						output[currValue-1] = turn;
						currx = nx;
						curry = ny;
						currValue--;
						break forloop;
					}
				}
			}
		}
		output[0] = computeTurn(currx, curry, 2, 2);

		
		
		return output;
		
	}
	
	/**
	 * given an array and a coordinate and a value, propagate value to the neighbors of the coordinate
	 * @param distanceArray
	 * @param y
	 * @param x
	 * @param value
	 */
	public static void propagate(int[][] distanceArray, int x, int y, int value) {
		int[][] neighbors = getNeighbors(x,y);
		for (int[] neighbor: neighbors) {
			int ny = neighbor[1];
			int nx = neighbor[0];
			if (ny < 5 && ny >= 0 && nx < 5 && nx >= 0) {
				if (distanceArray[ny][nx] > value || distanceArray[ny][nx] == 0) {
					distanceArray[ny][nx] = value;
					
				}
			}
		}
	}
	
	
	public static int[][] getNeighbors(int x, int y) {
		int[][] output = {{x-1, y}, {x-1, y+1}, {x, y+1}, {x+1, y+1},{x+1, y}, {x+1, y-1}, {x, y-1}, {x-1, y-1}};
		return output;
	}
	
	public static boolean validNeighbor(int[][] encArray, int y, int x) {
		if (y >= 5 || x >= 5 || y < 0 || x < 0) {
			return false;
		} else if (encArray[y][x] > 0) {
			return false;
		} else {
			return true;
		}
	}
	
	public static int computeTurn(int x, int y, int nx, int ny) {
		if (ny == y+1 && nx == x) {
			return 0;
		} else if (ny == y+1 && nx == x-1) {
			return 1;
		} else if (ny == y && nx == x-1) {
			return 2;
		} else if (ny == y-1 && nx == x-1) {
			return 3;
		} else if (ny == y-1 && nx == x) {
			return 4;
		} else if (ny == y-1 && nx == x+1) {
			return 5;
		} else if (ny == y && nx == x+1) {
			return 6;
		} else if (ny == y+1 && nx == x+1) {
			return 7;
		} else {
			return 8;
		}
	}
	
//	public static int[] getNeighborFromTurn(int x, int y, int turn) {
//		if (turn == 0) {
//			int[] output = {x-1, y};
//			return output;
//		} else if (turn == 1) {
//			int[] output = {x-1, y+1};
//			return output;
//		} else if (turn == 2) {
//			int[] output = {x, y+1};
//			return output;
//		} else if (turn == 3) {
//			int[] output = {x+1, y+1};
//			return output;
//		} else if (turn == 4) {
//			int[] output = {x+1, y};
//			return output;
//		} else if (turn == 5) {
//			int[] output = {x+1, y-1};
//			return output;
//		} else if (turn == 6) {
//			int[] output = {x, y-1};
//			return output;
//		} else if (turn == 7) {
//			int[] output = {x-1, y-1};
//			return output;
//		} else {
//			int[] output = {x, y};
//			return output;
//		}
//	}
	
	private static void printArray(int[][] array) {
		for (int i=0; i<5; i++) {
			System.out.println("Array:");
			System.out.println(array[i][0] + " " + array[i][1] + array[i][2] + " " + array[i][3] + " " + array[i][4]);
		}
	}
	
//	public static void main(String[] args) {
//		
//		int[][] array = {{0,0,0,0,0},{0,0,0,0,0},{0,0,0,0,0},{0,1,1,1,0},{0,0,0,1,0}};
//		
//		
//	}
}