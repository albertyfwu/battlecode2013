package base;

import java.util.ArrayList;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;

public class SoldierRobot extends BaseRobot {

	public Platoon platoon = null;
	public boolean calculatedPath = false;
	
	// TODO: Testing
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
			currentLocation = rc.getLocation();

			if (unassigned && rc.isActive()) {
				if (NavSystem.followingWaypoint) {
					NavSystem.followWaypoint();
//					rc.setIndicatorString(0, "following");
				} else {
					// do other stuff
					MapLocation end = rc.senseEnemyHQLocation();
//					NavSystem.calculateSmartWaypoint(end);
					NavSystem.calculateBackdoorWaypoint(end);
					NavSystem.followWaypoint();
//					rc.setIndicatorString(0, "not following");
				}
//				
//				Message message = BroadcastSystem.read(ChannelType.CHANNEL1);
//				if (message.isValid){
//					rc.setIndicatorString(0, Integer.toString(message.body));
//				}
			} else { // is assigned to an encampment job
				EncampmentJobSystem.updateJobTaken(assignedChannel);
				if (rc.isActive()) {
					if (rc.senseEncampmentSquare(currentLocation) && currentLocation.equals(goalLoc)) {
						rc.captureEncampment(RobotType.GENERATOR);
					} else {
						if (BFSMode) {
//							System.out.println("Direction: " + BFSTurns[BFSRound]);
							Direction dir = Direction.values()[BFSTurns[BFSRound]];
							if (rc.canMove(dir)) {
								rc.move(dir);
								BFSRound++;
							} else {
								BFSIdle++;
							}
							
						} else if (rc.getLocation().distanceSquaredTo(goalLoc) <= 8) {
							// first try to get closer
							boolean moved = NavSystem.moveCloser(goalLoc);
							System.out.println("moved: " + moved);
							if (moved == false) {
//								System.out.println("goalLoc.x: " + goalLoc.x);
//								System.out.println("goalLoc.y: " + goalLoc.y);
								
								
								BFSMode = true;
								int[][] encArray = NavSystem.populate5by5board();
								int[] goalCoord = NavSystem.locToIndex(rc.getLocation(), goalLoc, 2);
								BFSTurns = NavSystem.runBFS(encArray, goalCoord[1], goalCoord[0]);
								BFSRound = 0;
								
								print2Darray(encArray);
							}
						} else {
							NavSystem.goToLocation(goalLoc);
						}
					}
					
				}
				
			}
			
			

		} catch (Exception e) {
			// Deal with exception
		}
	}
	
	public Platoon getPlatoon() {
		return this.platoon;
	}
	
	private static void print2Darray(int[][] array) {
		for (int i=0; i<5; i++) {
			System.out.println("Array:");
			System.out.println(array[i][0] + " " + array[i][1] + array[i][2] + " " + array[i][3] + " " + array[i][4]);
		}
	}
}
