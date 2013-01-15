package swarm4;

import battlecode.common.*;

public class RobotPlayer{
	
	static RobotController rc;
	static MapLocation rallyPt = null;
	static int retreating = 10;
	static int attackTime = 500;
	static int mult = 2432;
	static MapLocation previousLocation = null;
	static boolean defuseMines = false;
	
	public static void run(RobotController myRC){
		rc = myRC;
		if (rc.getTeam()==Team.A){
			attackTime = 1000;
			mult = 515;
		}
		while(true){
			try{
				if (rc.getType()==RobotType.SOLDIER){
					soldierCode();
				}else{
					Direction toEnemy = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
					rallyPt = rc.getLocation().add(toEnemy,5);
					hqCode();
				}
			}catch (Exception e){
				System.out.println("caught exception before it killed us:");
				e.printStackTrace();
			}
			rc.yield();
		}
	}
	private static void soldierCode(){
		while(true){
			try{
				//see if I am stuck
				if (rc.getLocation()==previousLocation){
					defuseMines = true;
				}else{
					defuseMines = false;
				}
				previousLocation = rc.getLocation();
				
				//receive rally point from HQ
				MapLocation received = IntToMaplocation(rc.readBroadcast(getChannel()));
				if (received!= null){
					rallyPt = received;
				}
				
				//go to rally point or fight enemies
				Robot[] enemyRobots = rc.senseNearbyGameObjects(Robot.class,1000000,rc.getTeam().opponent());
				Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class,14,rc.getTeam());
				int tolerance = alliedRobots.length/2;//target goal distance tolerance
				if(enemyRobots.length==0){//no enemies nearby
					if (rallyPt!= null){
						rc.setIndicatorString(0,"current loc "+rc.getLocation().toString()+", goal "+rallyPt.toString());
						goToLocation(rallyPt,defuseMines,tolerance);
					}else{
						goToLocation(rc.senseEnemyHQLocation(),defuseMines,0);
					}
				}else{//someone spotted
					MapLocation closestEnemy = findClosest(enemyRobots);
					retreating--;
					if(retreating==-10)
						retreating =10;
					if(retreating>0){
						Direction dirToEnemy = rallyPt.directionTo(closestEnemy);
						MapLocation retreat = rallyPt.add(dirToEnemy,-10);
						goToLocation(retreat,false,10);
					}else{
						goToLocation(closestEnemy,false,0);
					}
				}
			}catch (Exception e){
				System.out.println("Soldier Exception");
				e.printStackTrace();
			}
			rc.yield();
		}
	}
	private static MapLocation findClosest(Robot[] enemyRobots) throws GameActionException {
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
		return closestEnemy;
	}
	private static void goToLocation(MapLocation whereToGo,boolean defuseMines,int tolerance) throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		int dist = myLoc.distanceSquaredTo(whereToGo);
		if (rc.isActive()){
			if (dist>tolerance){//try to move to target
				Direction dir = rc.getLocation().directionTo(whereToGo);
				int[] directionOffsets = {0,1,-1,2,-2};
				Direction lookingAtCurrently = null;
				lookAround: for (int d:directionOffsets){
					lookingAtCurrently = Direction.values()[(dir.ordinal()+d+8)%8];
					Team currentMine = rc.senseMine(myLoc.add(lookingAtCurrently));
					if(rc.canMove(lookingAtCurrently)&&(defuseMines||(!defuseMines&&(currentMine==rc.getTeam()||currentMine==null)))){
						moveOrDefuse(lookingAtCurrently);
						break lookAround;
					}
				}
			}else if (rc.senseMine(myLoc)==null){//close enough to target. Might as well lay some mines.
				if(rc.hasUpgrade(Upgrade.PICKAXE)){
					if(chessBoard(myLoc))
						rc.layMine();
				}else{
					if(checkerboard(myLoc))
						rc.layMine();
				}
			}
		}
	}
	private static boolean checkerboard(MapLocation m){
		return (m.x+m.y)%2==0;
	}
	private static boolean chessBoard(MapLocation m){
		return (2*m.x+m.y)%5==0;
	}
	private static void moveOrDefuse(Direction dir) throws GameActionException{
		MapLocation ahead = rc.getLocation().add(dir);
		Team mineAhead = rc.senseMine(ahead);
		if(mineAhead!=null&&mineAhead!= rc.getTeam()){
			rc.defuseMine(ahead);
		}else{
			rc.move(dir);			
		}
	}
	public static void hqCode() throws GameActionException{
		if (rc.isActive()) {
			// Spawn a soldier
			//			Robot[] alliedRobots = rc.senseNearbyGameObjects(Robot.class,100000,rc.getTeam());
			if(rc.getTeamPower()-40>10){
				lookAround: for (Direction d:Direction.values()){
					if (rc.canMove(d)){
						rc.spawn(d);
						break lookAround;
					}
				}
			}


		}
		//move the rally point
		if (Clock.getRoundNum()>attackTime){
			rallyPt = rc.senseEnemyHQLocation();
		}
		//message allies about where to go
		int channel = getChannel();
		int msg = MapLocationToInt(rallyPt);
		rc.broadcast(channel, msg);
		rc.setIndicatorString(0,"Posted "+msg+" to "+channel);
	}
	public static int getChannel(){
		int channel = (Clock.getRoundNum()*mult)%GameConstants.BROADCAST_MAX_CHANNELS;
		return channel;
	}
	public static int MapLocationToInt(MapLocation loc){
		return loc.x*1000+loc.y;
	}
	public static MapLocation IntToMaplocation(int mint){
		int y = mint%1000;
		int x = (mint-y)/1000;
		if(x==0&&y==0){
			return null;
		}else{
			return new MapLocation(x,y);
		}
	}
}