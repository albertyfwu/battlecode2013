package followleader;

import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import battlecode.common.Team;

/** The example funcs player is a player meant to demonstrate basic usage of the most common commands.
 * Robots will move around randomly, occasionally mining and writing useless messages.
 * The HQ will spawn soldiers continuously. 
 */
public class RobotPlayer {
	public static void run(RobotController rc) {
		while (true) {
			try {
				if (rc.getType() == RobotType.HQ) {
					if (rc.isActive()) {
						// Spawn a soldier
						Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
						if (rc.canMove(dir))
							rc.spawn(dir);
					}
				} else if (rc.getType() == RobotType.SOLDIER) {
					if (rc.isActive()) {
						if(rc.getRobot().getID() == 101) {
							Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
							Team isBomb = rc.senseMine(rc.getLocation().add(dir));
							if (isBomb == null || isBomb == rc.getTeam()) {
								if(rc.canMove(dir)) {
									rc.move(dir);
									rc.setIndicatorString(0, "Last direction moved: "+dir.toString());
								}
							} else {
								rc.defuseMine(rc.getLocation().add(dir));
							}
							
							MapLocation m = rc.getLocation();
							rc.broadcast(1, m.x);
							rc.broadcast(2, m.y);
						} else {
							 int x = rc.readBroadcast(1);
							 int y = rc.readBroadcast(2);
							 
							 Direction dir = rc.getLocation().directionTo(new MapLocation(x,y));
							 if(rc.canMove(dir)) {
									rc.move(dir);
									rc.setIndicatorString(0, "Last direction moved: "+dir.toString());
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
}
