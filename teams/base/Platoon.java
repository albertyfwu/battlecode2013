package base;

import java.util.ArrayList;

public class Platoon {
	
	public int id;
	public PlatoonStrategy strategy;
	public ArrayList<SoldierRobot> soldiers;
	
	public Platoon(int id, PlatoonStrategy strategy) {
		this.id = id;
		this.strategy = strategy;
		this.soldiers = new ArrayList<SoldierRobot>();
	}
	
	public Platoon(int id, PlatoonStrategy strategy, ArrayList<SoldierRobot> soldiers) {
		this.id = id;
		this.strategy = strategy;
		this.soldiers = soldiers;
	}
	
	public PlatoonStrategy getStrategy() {
		return this.strategy;
	}
	
}
