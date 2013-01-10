package medbay;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Upgrade;

public class RobotPlayer {
	
	public static RobotController rc;
	
	public static boolean buildingMedbay1 = false;
	public static boolean buildingMedbay2 = false;
	public static MapLocation[] medbayLocations = {null, null};
	
	public static void run(RobotController myRC) {
		rc = myRC;
		while (true) {
			try {
				if (rc.getType() == RobotType.HQ) {
					if (rc.isActive()) {
						if (!buildingMedbay1 || !buildingMedbay2) {
							if (medbayLocations[0] == null && medbayLocations[0] == null) {
								// Look for closest encampments
								MapLocation[] encampmentSquares = rc.senseAllEncampmentSquares();
								int[] minDistances = {Integer.MAX_VALUE, Integer.MAX_VALUE};
								for (MapLocation encampmentSquare : encampmentSquares) {
									int currentDistance = rc.getLocation().distanceSquaredTo(encampmentSquare); 
									if (currentDistance <= minDistances[0]) {
										minDistances[1] = minDistances[0];
										minDistances[0] = currentDistance;
										medbayLocations[1] = medbayLocations[0];
										medbayLocations[0] = encampmentSquare;
									}
								}
								// Broadcast the locations
								rc.broadcast(80, medbayLocations[0].x);
								rc.broadcast(81, medbayLocations[0].y);
								rc.broadcast(90, medbayLocations[1].x);
								rc.broadcast(91, medbayLocations[1].y);
							}
							// Go to these encampments and capture them
							if (!buildingMedbay1 && medbayLocations[0] != null) {
								Direction dir = rc.getLocation().directionTo(medbayLocations[0]);
								if (rc.canMove(dir)) {
									rc.spawn(dir);
									buildingMedbay1 = true;
								}
							} else if (!buildingMedbay2 && medbayLocations[1] != null) {
								Direction dir = rc.getLocation().directionTo(medbayLocations[1]);
								if (rc.canMove(dir)) {
									rc.spawn(dir);
									buildingMedbay2 = true;
								}
							}
						} else {
							// Spawn a soldier
							Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
							if (rc.canMove(dir)) {
								rc.spawn(dir);
							}
						}
					}
				} else if (rc.getType() == RobotType.SOLDIER) {
					if (rc.isActive()) {
						int x1 = rc.readBroadcast(80);
						int y1 = rc.readBroadcast(81);
						int x2 = rc.readBroadcast(90);
						int y2 = rc.readBroadcast(91);
						MapLocation medbayLocation = null;
						if (Math.random() < 0.5) {
							medbayLocation = new MapLocation(x1, y1);
						} else {
							medbayLocation = new MapLocation(x2, y2);
						}
						if (rc.getRobot().getID() < 106) {
							// Go to the encampment and become the encampment
							Direction dir = rc.getLocation().directionTo(medbayLocation);
							if (dir == Direction.OMNI){
								// build an encampment
								rc.captureEncampment(RobotType.MEDBAY);
							} else {
								goToLocation(medbayLocation);
							}
						} else {
							// Go to the encampment and lay mines along the way
							if (Math.random() < 0.05 && rc.senseMine(rc.getLocation()) == null) {
								// Lay a mine
								rc.layMine();
							} else {
								if (rc.senseNearbyGameObjects(Robot.class, 49, rc.getTeam()).length > 10) {
									if (rc.senseNearbyGameObjects(Robot.class, 14, rc.getTeam().opponent()).length > 0) {
										goToLocationAvoidMines(rc.senseEnemyHQLocation());
									} else {
										goToLocation(rc.senseEnemyHQLocation());
									}
								} else {
									goToLocation(medbayLocation);
								}
							}
						}
					}
				}
				// End turn
				rc.yield();
			} catch (Exception e) {
				e.printStackTrace();
			}
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
	
	/**
	 * helper fcn to help compute nearest enemy robot
	 * @param enemyRobots - must not be empty
	 * @return
	 * @throws GameActionException 
	 */
	private static int[] getClosestEnemy(Robot[] enemyRobots) throws GameActionException {
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
		int[] output = new int[3];
		output[0] = closestDist;
		output[1] = closestEnemy.x;
		output[2] = closestEnemy.y;
		
		return output;
	}
}
