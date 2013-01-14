package base;

import java.util.ArrayList;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class SoldierRobot extends BaseRobot {
	
	public Platoon platoon;
	
	public SoldierState soldierState;
	
	// For mining
	private MapLocation miningCenter;
	private int miningRadius;
	
	public ArrayList<MapLocation> wayPoints;
	public int wayPointsSize;
	public int wayPointsIndex;
	
	public boolean unassigned = true;
	public ChannelType assignedChannel;
	public MapLocation goalLoc;
	
	public MapLocation currentLocation;
	
	
	private static boolean BFSMode = false;
	private static int BFSRound = 0;
	private static int[] BFSTurns;
	private static int BFSIdle = 0;

	
	
	public SoldierRobot(RobotController rc) throws GameActionException {
		super(rc);
		
		ChannelType channel = EncampmentJobSystem.findJob();
		if (channel != null) {
			assignedChannel = channel;
			unassigned = false;
			goalLoc = EncampmentJobSystem.goalLoc;
			System.out.println("channel: " + channel);
			System.out.println("goalLocx: " + goalLoc.x);
			System.out.println("goalLocy: " + goalLoc.y);
			EncampmentJobSystem.updateJobTaken(channel);
		}
	}
	
//	public SoldierRobot(RobotController rc, Platoon platoon) {
//		super(rc);
//		this.platoon = platoon;
//	}

	@Override
	public void run() {
		try {
			if (Clock.getRoundNum() < 300) {
				if (soldierState != SoldierState.MINING_IN_CIRCLE) {
					setupCircleMining(new MapLocation(20, 20));
				}
				mineInCircle();
			} else {
//				if (NavSystem.navMode == NavMode.NEUTRAL) {
//					NavSystem.setupSmartNav(rc.senseEnemyHQLocation());
//				}
//				NavSystem.followWaypoints();
				rc.suicide();
			}
			
			
//			if (NavSystem.navMode == NavMode.NEUTRAL) {
//				NavSystem.setupSmartNav(new MapLocation(29, 27));
//				NavSystem.followWaypoints();
//			} else {
//				NavSystem.followWaypoints();
//			}
				
//			currentLocation = rc.getLocation();
//
//			if (unassigned && rc.isActive()) {
////				if (NavSystem.navMode == NavMode.NEUTRAL) {
////					NavSystem.setupBackdoorNav(new MapLocation(20, 20));
////					NavSystem.followWaypoints();
////				} else {
////					NavSystem.followWaypoints();
////				}
//				
//				NavSystem.goToLocation(new MapLocation(20, 20));
//			} else { // is assigned to an encampment job
//				if (!unassigned) { // if assigned to something
//					EncampmentJobSystem.updateJobTaken(assignedChannel);
//				}
//				if (rc.isActive()) {
//					if (rc.senseEncampmentSquare(currentLocation) && currentLocation.equals(goalLoc)) {
//						rc.captureEncampment(RobotType.GENERATOR);
//					} else {
//						if (BFSMode) {
//							if (BFSIdle >= 50) { // if idle for 50 turns or more
//								BFSMode = false;
//							} else {
//								System.out.println("Direction: " + BFSTurns[BFSRound]);
//								Direction dir = Direction.values()[BFSTurns[BFSRound]];
//								if (rc.canMove(dir)) {
//									rc.move(dir);
//									BFSRound++;
//								} else {
//									BFSIdle++;
//								}
//							}
//							
//							
//						} else if (rc.getLocation().distanceSquaredTo(goalLoc) <= 8) {
//							// first try to get closer
//							BFSIdle = 0;
//							boolean moved = NavSystem.moveCloser(goalLoc);
//							System.out.println("moved: " + moved);
//							if (moved == false) {
////								System.out.println("goalLoc.x: " + goalLoc.x);
////								System.out.println("goalLoc.y: " + goalLoc.y);
//								
//								
//								BFSMode = true;
//								int[][] encArray = NavSystem.populate5by5board();
//								int[] goalCoord = NavSystem.locToIndex(rc.getLocation(), goalLoc, 2);
//								BFSTurns = NavSystem.runBFS(encArray, goalCoord[1], goalCoord[0]);
//								BFSRound = 0;
//								
//								print2Darray(encArray);
//							}
//						} else {
//							NavSystem.goToLocation(goalLoc);
//						}
//					}
//					
//				}
//				
//			}
			
			
		} catch (Exception e) {
			System.out.println("caught exception before it killed us:");
			System.out.println(rc.getRobot().getID());
			e.printStackTrace();
		}
	}
	
	public Platoon getPlatoon() {
		return this.platoon;
	}
	
	/**
	 * Set up a center MapLocation for mining in a circle
	 * @param center
	 */
	private void setupCircleMining(MapLocation center) {
		miningCenter = center;
		miningRadius = 2;
	}
	
	private void mineInCircle() throws GameActionException {
		int radiusSquared = miningRadius * miningRadius;
		if (minesDenselyPacked(miningCenter, miningRadius)) {
			// mines are fairly dense, so expand the circle in which to mine
			miningRadius += Constants.MINING_RADIUS_DELTA;
			System.out.println("start");
			System.out.println("miningRadius: " + miningRadius);
			System.out.println("mines: " + rc.senseMineLocations(miningCenter, radiusSquared, rc.getTeam()).length);
		}
		if (rc.getLocation().distanceSquaredTo(miningCenter) >= radiusSquared) {
			NavSystem.goToLocation(miningCenter);
		} else {
			// Lay a mine if possible
			if (rc.getLocation().distanceSquaredTo(miningCenter) <= radiusSquared && rc.senseMine(rc.getLocation()) == null) {
				rc.layMine();
			}
			// Walk around the circle
			Direction dir = rc.getLocation().directionTo(miningCenter).rotateLeft().rotateLeft(); // move counterclockwise around circle
			NavSystem.goDirectionAndDefuse(dir);
		}
	}
	
	/**
	 * Given a center MapLocation and a radiusSquared, returns true if the circle is densely packed with allied mines.
	 * @param center
	 * @param radiusSquared
	 * @return
	 */
	private boolean minesDenselyPacked(MapLocation center, int miningRadius) {
		int radiusSquared = miningRadius * miningRadius;
		return rc.senseMineLocations(center, radiusSquared, rc.getTeam()).length >= 1 * radiusSquared;
	}
	
	private static void print2Darray(int[][] array) {
		for (int i=0; i<5; i++) {
			System.out.println("Array:");
			System.out.println(array[i][0] + " " + array[i][1] + array[i][2] + " " + array[i][3] + " " + array[i][4]);
		}
	}
}
