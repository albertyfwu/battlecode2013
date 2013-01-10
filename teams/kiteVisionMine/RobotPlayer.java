package kiteVisionMine;

import battlecode.common.*;

public class RobotPlayer{
	
	private static RobotController rc;
	private static MapLocation rallyPoint;
	private static int attackTime;
	
	public static void run(RobotController myRC){
		rc = myRC;
		rallyPoint = findRallyPoint();
		attackTime = 100000;
		
		while(true){
			try{
				if (rc.getType()==RobotType.SOLDIER){
					Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,1000000,rc.getTeam().opponent());
					if (getAttackTime() > 0) {
						attackTime = getAttackTime();
					}
					if(enemyRobots.length==0 || (enemyRobots.length == 1 && rc.senseRobotInfo(enemyRobots[0]).type == RobotType.HQ)){//no enemies nearby
						Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class, 1000000, rc.getTeam());
						
						if (alliedRobots.length < 25){
							attackTime = 100000;
							if (Math.random() < 0.1 && alliedRobots.length < 20) {
								if (rc.senseMine(rc.getLocation()) == null) {
									rc.layMine();
								}
							}
							goToLocation(rallyPoint);
						}else{
							goToLocation(rc.senseEnemyHQLocation());
						}
					}else{//someone spotted
						int closestDist = 1000000;
						MapLocation closestEnemy=null;
						for (int i=0;i<enemyRobots.length;i++){
							Robot arobot = enemyRobots[i];
							RobotInfo arobotInfo = rc.senseRobotInfo(arobot);
							int dist = arobotInfo.location.distanceSquaredTo(rc.getLocation());
							if (dist<closestDist){
								closestDist = dist;
								closestEnemy = arobotInfo.location;
							}
						}
						if (Clock.getRoundNum() < attackTime) { // if still kiting
							if (closestDist < 32) {
								Direction dir = rc.getLocation().directionTo(getEnemyCenter()).opposite();
								goDirectionAvoidMines(dir);
								
							} else {
								goToLocationAvoidMines(closestEnemy);
							}
						} else {
							goToLocationAvoidMines(closestEnemy);
						}
						
						
					}
				}else{
					hqCode();
				}
			}catch (Exception e){
				System.out.println("caught exception before it killed us:");
				e.printStackTrace();
			}
			rc.yield();
		}
	}
	
	private static void goToLocation(MapLocation whereToGo) throws GameActionException {
		int dist = rc.getLocation().distanceSquaredTo(whereToGo);
		if (dist>0&&rc.isActive()){
			Direction dir = rc.getLocation().directionTo(whereToGo);
			goDirectionAndDefuse(dir);
		}
	}
	
	private static void goToLocationAvoidMines(MapLocation whereToGo) throws GameActionException {
		int dist = rc.getLocation().distanceSquaredTo(whereToGo);
		if (dist>0&&rc.isActive()){
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
		lookAround: for (int d:directionOffsets){
			lookingAtCurrently = Direction.values()[(dir.ordinal()+d+8)%8];
			if(rc.canMove(lookingAtCurrently)){
				if (!badBomb(rc.getLocation().add(lookingAtCurrently))) {
					rc.move(lookingAtCurrently);
					rc.setIndicatorString(0, "Last direction moved: "+lookingAtCurrently.toString());
				}
				break lookAround;
			}
		}
	}
	
	

	private static MapLocation findRallyPoint() {
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
		MapLocation ourLoc = rc.senseHQLocation();
		int x = (enemyLoc.x+3*ourLoc.x)/4;
		int y = (enemyLoc.y+3*ourLoc.y)/4;
		MapLocation rallyPoint = new MapLocation(x,y);
		return rallyPoint;
	}

	public static void hqCode() throws GameActionException{
		// send broadcasts
		sendEnemyCenter();
		if (attackTime == 100000) {
			sendAttackTime();
		}
		if (!rc.hasUpgrade(Upgrade.VISION)) {
			rc.researchUpgrade(Upgrade.VISION);
		}
		if (rc.isActive()) {
			// Spawn a soldier
			Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
			if (rc.canMove(dir)) {
				rc.spawn(dir);
			}
		}
		

	}
	
	public static void sendAttackTime() throws GameActionException {
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,1000000,rc.getTeam().opponent());
		if (enemyRobots.length > 0) {
			attackTime = Clock.getRoundNum()+7;
			rc.broadcast(3, attackTime);
			System.out.println("attacktime");
		}
		
		
	}
	
	public static int getAttackTime() throws GameActionException {
		attackTime = rc.readBroadcast(3);
		if (attackTime > 0) {
			return attackTime;
		}
		return 0;
	}
	
	public static void sendEnemyCenter() throws GameActionException {
		Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,1000000,rc.getTeam().opponent());
		if (enemyRobots.length > 0) {
			MapLocation sum = new MapLocation (0,0);
			for (int i=0;i<enemyRobots.length;i++){
				Robot arobot = enemyRobots[i];
				RobotInfo arobotInfo = rc.senseRobotInfo(arobot);
				sum = sum.add(arobotInfo.location.x, arobotInfo.location.y);
			}
			rc.broadcast(1, sum.x/enemyRobots.length);
			rc.broadcast(2, sum.y/enemyRobots.length);	
		}
	}
	
	public static MapLocation getEnemyCenter() throws GameActionException {
		int x = rc.readBroadcast(1);
		int y = rc.readBroadcast(2);
		return new MapLocation(x,y);
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
	
}