package base;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.Upgrade;

public class HQRobot extends BaseRobot {

	public Upgrade[] upgrades;
	HQStrategy strategy;
	
	public HQRobot(RobotController rc, HQStrategy strategy) {
		super(rc);
		this.strategy = strategy;
	}

	@Override
	public void run() {
		try {
			// Spawn a soldier
			Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
			if (rc.canMove(dir)) {
				rc.spawn(dir);
			}
			
//			BroadcastSystem.write(ChannelType.CHANNEL1, 100);
			
//			BroadcastChannel channel = BroadcastSystem.getChannelByType(ChannelType.values()[0]);
//			Message message = new Message(0, Clock.getRoundNum());
//			channel.write(rc, message);
			
//			// TODO: find out what strategy to switch to?
//			// this.strategy = HQStrategy.xxx
//			if (rc.isActive()) {
//				switch (strategy) {
//				case CREATE_SOLDIER:
//					create_soldier();
//					break;
//				case RESEARCH_DEFUSION:
//					if (!rc.hasUpgrade(Upgrade.DEFUSION)) {
//						rc.researchUpgrade(Upgrade.DEFUSION);
//					}
//					break;
//				case RESEARCH_FUSION:
//					if (!rc.hasUpgrade(Upgrade.FUSION)) {
//						rc.researchUpgrade(Upgrade.FUSION);
//					}
//					break;
//				case RESEARCH_NUKE:
//					if (!rc.hasUpgrade(Upgrade.NUKE)) {
//						rc.researchUpgrade(Upgrade.NUKE);
//					}
//					break;
//				case RESEARCH_PICKAXE:
//					if (!rc.hasUpgrade(Upgrade.PICKAXE)) {
//						rc.researchUpgrade(Upgrade.PICKAXE);
//					}
//					break;
//				case RESEARCH_VISION:
//					if (!rc.hasUpgrade(Upgrade.VISION)) {
//						rc.researchUpgrade(Upgrade.VISION);
//					}
//					break;
//				default:
//					break;
//				}
//			}
		} catch (Exception e) {
			//
		}
	}
	
	public void create_soldier() throws GameActionException {
		// Spawn a soldier
		Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
		if (rc.canMove(dir)) {
			rc.spawn(dir);
		}
	}
}
