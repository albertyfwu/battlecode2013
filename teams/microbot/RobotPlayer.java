package microbot;

import battlecode.common.*;

public class RobotPlayer{
	
	private static RobotController rc;
	private static MapLocation rallyPoint;
	
	public static void run(RobotController myRC){
		rc = myRC;
		rallyPoint = findRallyPoint();
		while(true){
			try{
				if (rc.getType()==RobotType.SOLDIER){
					if (Clock.getRoundNum()<100){
						goToLocation(rallyPoint);
					}else{
						 microCode();
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
	
	public static void microCode() throws GameActionException {
		Robot[] enemiesList = rc.senseNearbyGameObjects(Robot.class, 100000, rc.getTeam().opponent());
        int[] closestEnemyInfo = getClosestEnemy(enemiesList);
        MapLocation closestEnemyLocation = new MapLocation(closestEnemyInfo[1], closestEnemyInfo[2]);
        int enemyDistSquared = closestEnemyLocation.distanceSquaredTo(rc.getLocation());

        if (enemyDistSquared <= 2) { // if there is enemy in one dist
        	Robot[] enemiesInOneDist = rc.senseNearbyGameObjects(Robot.class, 2, rc.getTeam().opponent());
        	// stay
        	rc.setIndicatorString(0, "stay1");
        } else if (enemyDistSquared == 16 || enemyDistSquared > 18){ // if no enemies in one, two, or three dist
        	goToLocation(closestEnemyLocation);
        } else { // enemies in two or three dist
        	double[] our23 = getEnemies2Or3StepsAway();
            double[] enemy23 = getEnemies2Or3StepsAwaySquare(closestEnemyLocation, rc.getTeam().opponent());
            rc.setIndicatorString(2, our23[0] + " " + our23[1] + " " + our23[2]);

            rc.setIndicatorString(1, enemy23[0] + " " + enemy23[1] + " " + enemy23[2]);
            if (our23[1] > 0) { // closest enemy in 2 dist
            	if (enemy23[1] + enemy23[0] > our23[1] + our23[2]) {
            		// move forward
            		goToLocation(closestEnemyLocation);
            		rc.setIndicatorString(0, "forward2");
            	} else if (enemy23[0] + enemy23[1] + enemy23[2] > our23[1] + our23[2]) {
            		rc.setIndicatorString(0, "back2.5");
            		goAwayFromLocation(closestEnemyLocation);
            		//back
            	} else {
            		goAwayFromLocation(closestEnemyLocation);
            		rc.setIndicatorString(0, "back2");
            	}
            } else { // closest enemy is 3 dist
            	if (enemy23[1] > 0) { // if enemy 2dist is > 0
            		int closestDist = 100;
            		int dist;
            		MapLocation closestAllyLocation = null;
            		Robot[] twoDistAllies = rc.senseNearbyGameObjects(Robot.class, closestEnemyLocation, 8, rc.getTeam());
            		for (Robot ally: twoDistAllies) {
            			RobotInfo arobotInfo = rc.senseRobotInfo(ally);
            			dist = arobotInfo.location.distanceSquaredTo(rc.getLocation());
            			if (dist<closestDist){
            				closestDist = dist;
            				closestAllyLocation = arobotInfo.location;
            			}
            		}
            		
            		double[] ally23 = getEnemies2Or3StepsAwaySquare(closestAllyLocation, rc.getTeam());
            		
                	if (enemy23[0] + enemy23[1] + enemy23[2] > ally23[1] + ally23[2]) {
                		goToLocation(closestEnemyLocation);
                		rc.setIndicatorString(0, "forward3");
                	} else {
                		rc.setIndicatorString(0, "stay3");
                	}
            	}  else if (enemy23[0] > 0) {
            		goToLocation(closestEnemyLocation);
            		rc.setIndicatorString(0, "forward4");
            	} else {
            		rc.setIndicatorString(0, "stay4");
            	}
            }
        }
	}
	

    
    private static double[] getEnemies2Or3StepsAway() throws GameActionException {
    	double count1 = 0;
        double count2 = 0;
        double count3 = 0;
        Robot[] enemiesInVision = rc.senseNearbyGameObjects(Robot.class, 18, rc.getTeam().opponent());
        for (Robot enemy: enemiesInVision) {
                RobotInfo rinfo = rc.senseRobotInfo(enemy);
                int dist = rinfo.location.distanceSquaredTo(rc.getLocation());
                if (rinfo.type == RobotType.SOLDIER) {
                		if (dist <= 2) {
                			count1++;
                		} else if (dist <=8) {
                                count2++;
                        } else if (dist > 8 && (dist <= 14 || dist == 18)) {
                                count3++;
                        }
                } else {
                	if (dist <= 2) {
            			count1++;
                	} else if (dist <=8) {
                            count2 += 1;
                    } else if (dist > 8 && (dist <= 14 || dist == 18)) {
                            count3 += 1;
                    }
                }
               
        }
        
        double[] output = {count1, count2, count3};
        return output;
}

private static double[] getEnemies2Or3StepsAwaySquare(MapLocation square, Team squareTeam) throws GameActionException {
	double count1 = 0;
	double count2 = 0;
	double count3 = 0;
	Robot[] enemiesInVision = rc.senseNearbyGameObjects(Robot.class, square, 18, squareTeam.opponent());
	for (Robot enemy: enemiesInVision) {
		RobotInfo rinfo = rc.senseRobotInfo(enemy);
		int dist = rinfo.location.distanceSquaredTo(square);
		if (rinfo.type == RobotType.SOLDIER) {
			if (dist <= 2) {
				count1++;
			} else if (dist <=8) {
				count2++;
			} else if (dist <= 14 || dist == 18) {
				count3++;
			}
		} else {
			if (dist <= 2) {
				count1++;
			} else if (dist <=8) {
				count2 += 1;
			} else if (dist <= 14 || dist == 18) {
				count3 += 1;
			}
		}
	}

	int selfDist = square.distanceSquaredTo(rc.getLocation());
	
	if (selfDist <= 2) {
		count1++;
	} else if (selfDist<=8) {
		count2++;
	} else if (selfDist <= 14 || selfDist == 18) {
		count3++;
	}

	double[] output = {count1, count2, count3};
	return output;
}

public static int[] getClosestEnemy(Robot[] enemyRobots) throws GameActionException {
	int closestDist = rc.getLocation().distanceSquaredTo(rc.senseEnemyHQLocation());
	MapLocation closestEnemy=rc.senseEnemyHQLocation(); // default to HQ

	int dist = 0;
	for (int i=0;i<enemyRobots.length;i++){
		RobotInfo arobotInfo = rc.senseRobotInfo(enemyRobots[i]);
		dist = arobotInfo.location.distanceSquaredTo(rc.getLocation());
		if (dist<closestDist){
			closestDist = dist;
			closestEnemy = arobotInfo.location;
		}
	}
	int[] output = new int[4];
	output[0] = closestDist;
	output[1] = closestEnemy.x;
	output[2] = closestEnemy.y;		
	return output;
}

public static void goAwayFromLocation(MapLocation location) throws GameActionException {
	Direction dir = rc.getLocation().directionTo(location).opposite();
	if (dir != Direction.OMNI) {
		goDirectionAndDefuse(dir);
	}
}


}