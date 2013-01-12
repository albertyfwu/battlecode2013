package base;

import java.util.ArrayList;

import battlecode.common.Clock;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class SoldierRobot extends BaseRobot {

	public Platoon platoon = null;
	public boolean calculatedPath = false;
	
	// TODO: Testing
	public ArrayList<MapLocation> wayPoints;
	public int wayPointsSize;
	public int wayPointsIndex;
	
	public boolean unassiged = true;
	public int assignedChannel;
	
	
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
			if (NavSystem.followingWaypoint) {
				NavSystem.followWaypoint();
//				rc.setIndicatorString(0, "following");
			} else {
				// do other stuff
				MapLocation end = rc.senseEnemyHQLocation();
//				NavSystem.calculateSmartWaypoint(end);
				NavSystem.calculateBackdoorWaypoint(end);
				NavSystem.followWaypoint();
//				rc.setIndicatorString(0, "not following");
			}
//			
//			Message message = BroadcastSystem.read(ChannelType.CHANNEL1);
//			if (message.isValid){
//				rc.setIndicatorString(0, Integer.toString(message.body));
//			}
			

		} catch (Exception e) {
			// Deal with exception
		}
	}
	
	public Platoon getPlatoon() {
		return this.platoon;
	}
}
