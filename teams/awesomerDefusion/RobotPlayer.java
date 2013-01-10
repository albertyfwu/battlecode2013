package awesomerDefusion;

import battlecode.common.*;

public class RobotPlayer{
	
	private static RobotController rc;
	private static MapLocation rallyPoint;
	private static boolean enemyHasMines = false;
	
	public static void run(RobotController myRC){
		rc = myRC;
		rallyPoint = findRallyPoint();
		while(true){
			try{
				if (rc.getType()==RobotType.SOLDIER){
					Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,100000,rc.getTeam().opponent());
					if(enemyRobots.length==0 || (enemyRobots.length == 1 && rc.senseRobotInfo(enemyRobots[0]).type == RobotType.HQ)){//no enemies nearby
						Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class, 1000000, rc.getTeam());
						if (alliedRobots.length < 25){ // if < 25 allied robots
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
						int closestDist = 100000;
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
						Robot[] nearbyAllies= rc.senseNearbyGameObjects(Robot.class,64,rc.getTeam());
						if (nearbyAllies.length > 3 * enemyRobots.length) {
							if (closestDist > 4) {
								if (rc.hasUpgrade(Upgrade.DEFUSION) && Math.random() < 0.1) {
									MapLocation[] mines = rc.senseMineLocations(rc.getLocation(), 14, rc.getTeam().opponent());
									if (mines.length > 0) {
										rc.defuseMine(mines[0]);
									}
								}
								goToLocation(closestEnemy);
							}
						}
						goToLocationAvoidMines(closestEnemy);
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
				rangedDefuseMine();
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

	private static MapLocation findRallyPoint() {
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
		MapLocation ourLoc = rc.senseHQLocation();
		int x = (enemyLoc.x+3*ourLoc.x)/4;
		int y = (enemyLoc.y+3*ourLoc.y)/4;
		MapLocation rallyPoint = new MapLocation(x,y);
		return rallyPoint;
	}

	public static void hqCode() throws GameActionException{
		Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class,100000,rc.getTeam());
		if (alliedRobots.length > 10 && Clock.getRoundNum() > 0) {
			if (!rc.hasUpgrade(Upgrade.DEFUSION)) {
				rc.researchUpgrade(Upgrade.DEFUSION);
			}
		}
		if (rc.isActive()) {
			// Spawn a soldier
				Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
				if (rc.canMove(dir))
					rc.spawn(dir);
			
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
}