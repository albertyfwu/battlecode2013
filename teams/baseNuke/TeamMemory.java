package baseNuke;

import battlecode.common.RobotController;

public class TeamMemory {

	public static RobotController rc;
	public static long[] lastTeamMemory;
	
	public static void init(RobotController myRC) {
		rc = myRC;
	}
	
	public static void setMemory(TeamMemoryType teamMemoryType, long value) {
		rc.setTeamMemory(teamMemoryType.ordinal(), value);
	}
	
	public static void setMemory(TeamMemoryType teamMemoryType, long value, long mask) {
		rc.setTeamMemory(teamMemoryType.ordinal(), value, mask);
	}
	
	public static long[] getTeamMemory() {
		if (lastTeamMemory == null) {
			lastTeamMemory = rc.getTeamMemory();
		}
		return lastTeamMemory;
	}
	
	public static long getTeamMemory(TeamMemoryType teamMemoryType) {
		if (lastTeamMemory == null) {
			lastTeamMemory = rc.getTeamMemory();
		}
		return lastTeamMemory[teamMemoryType.ordinal()];
	}
	
}