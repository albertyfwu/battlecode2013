package macrobotDynamicRally;

import battlecode.common.*;

public class RobotPlayer{
	
	private static RobotController rc;
	private static MapLocation rallyPoint = null;
	private static boolean enemyHasMines = false;
	
	private static int designatedCapturer = 0;
	private static int capturerWaitCounter = 0;
	private static boolean isCapturer = false;
	private static MapLocation nextEncampment = null;
	
	private static int currentChannel1338 = 0;
	
	private static int numEncampments = 0; 
	
	private static MapLocation enemyHQLocation = null;
	private static MapLocation HQLocation = null;
	private static int rushDistanceSquared = 0;
	
	private static int numTotalEncampments = 10000;
	
	private static double ratioSuppliers = 0.8;
	
	
	public static void run(RobotController myRC){
		rc = myRC;
		while(true){
			try{
				// initialize HQ locations
				if (enemyHQLocation == null) {
					enemyHQLocation = rc.senseEnemyHQLocation();
				}
				if (HQLocation == null) {
					HQLocation = rc.senseHQLocation();
				}
				if (rushDistanceSquared == 0) {
					rushDistanceSquared = rc.senseHQLocation().distanceSquaredTo(rc.senseEnemyHQLocation());
				}
				
				int captureID = rc.readBroadcast(4338);
				if (rc.getRobot().getID() == captureID) {
					isCapturer = true;
				}
				
				if (rc.getType()==RobotType.SOLDIER && isCapturer == false){
					if (rc.isActive()) {
//						if (rallyPoint == null) {
//						int hashedLoc = rc.readBroadcast(4001);
//						if (hashedLoc != 0) {
//							int y = hashedLoc % 1000;
//							int x = (hashedLoc - y)/1000;
//							rallyPoint = new MapLocation(x,y);
//						}
//					}
					
						Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,100000,rc.getTeam().opponent());
						if(enemyRobots.length==0 || (enemyRobots.length == 1 && rc.senseRobotInfo(enemyRobots[0]).type == RobotType.HQ)){//no enemies nearby
							Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class, 1000000, rc.getTeam());
							numEncampments = rc.readBroadcast(8000);
							numTotalEncampments = rc.readBroadcast(8001);
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
		int[] directionOffsets = {0,1,-1,2,-2};
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
		int[] directionOffsets = {0,1,-1,2,-2};
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
		currentChannel1338 += 1;
		rc.broadcast(1338, currentChannel1338);
		if (rc.isActive()) {
			if (nextEncampment == null) {
				int hashedLoc = rc.readBroadcast(4001);
				if (hashedLoc != 0) {
					int y = hashedLoc % 1000;
					int x = (hashedLoc - y)/1000;
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
		
		
//		if (Clock.getRoundNum() > 50) { // for testing purposes
//			rc.resign();
//		} 
		// Broadcast nearest encampment
		MapLocation[] neutralEncampments = rc.senseEncampmentSquares(rc.getLocation(), 1000000,Team.NEUTRAL);
		if (numTotalEncampments == 10000) {
			numTotalEncampments = neutralEncampments.length;
		}
		MapLocation closestEnc = getClosestEncampemnt(neutralEncampments);

		rc.broadcast(4001, closestEnc.x * 1000 + closestEnc.y); // broadcast closestEnc location
		
		Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class,100000,rc.getTeam());
		
		
		MapLocation[] alliedEncampments = rc.senseEncampmentSquares(rc.getLocation(), 1000000, rc.getTeam());
		numEncampments = alliedEncampments.length;
		
		rc.broadcast(8000, numEncampments);
		rc.broadcast(8001, numTotalEncampments);
		
		int numAlliedSoldiers = alliedRobots.length - alliedEncampments.length;
		if (numAlliedSoldiers > 5) {
			if (designatedCapturer == 0) {
				int[] closeRobotInfo = getClosestSoldier(alliedRobots);
				designatedCapturer = closeRobotInfo[3];		
			} else {
				if (rc.readBroadcast(1338) == currentChannel1338) { // if it doesnt change
					capturerWaitCounter++;
				} else {
					currentChannel1338 = rc.readBroadcast(1338);
					capturerWaitCounter = 0;
				}
				
				if (capturerWaitCounter == 5) { // if you don't hear back from him
					designatedCapturer = 0; // reset
				}
			}
			rc.broadcast(4338, designatedCapturer);
		}	

		
		if (rc.isActive()) {
			if (numAlliedSoldiers > 25 && Clock.getRoundNum() > 500) {
				if (!rc.hasUpgrade(Upgrade.PICKAXE)) {
					rc.researchUpgrade(Upgrade.PICKAXE);
				} else if (!rc.hasUpgrade(Upgrade.FUSION)) {
					rc.researchUpgrade(Upgrade.FUSION);
				} else if (!rc.hasUpgrade(Upgrade.DEFUSION)) {
					rc.researchUpgrade(Upgrade.DEFUSION);
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
}