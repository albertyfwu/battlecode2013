package base;

import java.util.ArrayList;

import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class SoldierRobot extends BaseRobot {
	
	public Platoon platoon;
	public boolean calculatedPath = false;
	
	public ArrayList<MapLocation> wayPoints;
	public int wayPointsSize;
	public int wayPointsIndex;
	
	public boolean unassiged = true;
	public int assignedChannel;
	
	public SoldierRobot(RobotController rc) {
		super(rc);
	}

	@Override
	public void run() {
		try {
			if (NavSystem.navMode == NavMode.NEUTRAL) {
				NavSystem.setupSmartNav(NavSystem.enemyHQLocation);
				NavSystem.followWaypoints();
			} else {
				NavSystem.followWaypoints();
			}
		} catch (Exception e) {
			// Deal with exception
		}
	}
}
