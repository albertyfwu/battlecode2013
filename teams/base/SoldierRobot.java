package base;

import java.util.ArrayList;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

public class SoldierRobot extends BaseRobot {

	public Platoon platoon = null;
	public boolean calculatedPath = false;
	
	// TODO: Testing
	public ArrayList<MapLocation> wayPoints;
	public int wayPointsSize;
	public int wayPointsIndex;
	
	public SoldierRobot(RobotController rc) {
		super(rc);
	}
	
	public SoldierRobot(RobotController rc, Platoon platoon) {
		super(rc);
		this.platoon = platoon;
	}

	@Override
	public void run() {
		try {
//			Message message = BroadcastSystem.read(ChannelType.CHANNEL1);
//			if (message.isValid){
//				rc.setIndicatorString(0, Integer.toString(message.body));
//			}

			// Check to see if we're already moving along waypoints
			if (NavSystem.followingWaypoints) {
				NavSystem.followWaypoints();
//				rc.setIndicatorString(0, "following");
			} else {
				// do other stuff
				MapLocation end = rc.senseEnemyHQLocation();
				NavSystem.calculateSmartWaypoint(end);
				NavSystem.followWaypoints();
//				rc.setIndicatorString(0, "not following");
			}
			
			// Try to go to a coordinate
			// Try to go to (39, 27) on choice.xml map
//			MapLocation end = new MapLocation(39, 27);
//			MapLocation start = rc.getLocation();
//			if (!calculatedPath) {
//				calculatedPath = true;
//				wayPoints = Nav.calculatePath(rc, start, end);
//				wayPointsSize = wayPoints.size();
//			}
//
//			// TODO: write a function for following waypoints
//			if (wayPoints != null) {
//				// we have waypoints, so follow them
//				if (rc.getLocation().distanceSquaredTo(wayPoints.get(wayPointsIndex)) <= 5) {
//					if (wayPointsIndex < wayPointsSize - 1) {
//						wayPointsIndex++;
//					}
//				}
//				goToLocation(wayPoints.get(wayPointsIndex));
//			}
			
			
//			// TODO: do some broadcast reading, listen to leader of platoon, etc...
//			switch (this.platoon.getStrategy()) {
//			case KITE:
//				// 
//				break;
//			default:
//				break;
//			}
//			// TODO: check if we should change the strategy?
		} catch (Exception e) {
			// Deal with exception
		}
	}
	
	public Platoon getPlatoon() {
		return this.platoon;
	}
}
