package team162;

import battlecode.common.GameConstants;
import battlecode.common.MapLocation;

public class FastLocMapDouble {
    private static final int HASH = Math.max(GameConstants.MAP_MAX_WIDTH, GameConstants.MAP_MAX_HEIGHT);
    private double[][] map = new double[HASH][HASH];
    public boolean uninitialized = true;

    public void set(MapLocation loc, double value) {
        int x = loc.x % HASH;
        int y = loc.y % HASH;
        map[x][y] = value;
    }
    
    public double get(MapLocation loc) {
    	return map[loc.x % HASH][loc.y % HASH];
    }

    public void remove(MapLocation loc) {
        int x = loc.x % HASH;
        int y = loc.y % HASH;
        map[x][y] = 0;
    }

    public boolean contains(MapLocation loc) {
        return map[loc.x % HASH][loc.y % HASH] != 0;
    }
    
    public void clear() {
        map = new double[HASH][HASH];
    }
}