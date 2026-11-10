package com.natan.vortex.nav;

import net.minecraft.core.BlockPos;

import java.util.List;

/** Where the pathfinder should end up. Positions are the player's feet block. */
public interface Goal {
    double WALK = 4.633;

    boolean isGoal(int x, int y, int z);

    double heuristic(int x, int y, int z);

    String describe();

    static double horizontal(int dx, int dz) {
        dx = Math.abs(dx); dz = Math.abs(dz);
        int diag = Math.min(dx, dz), straight = Math.max(dx, dz) - diag;
        return (straight + diag * Math.sqrt(2)) * WALK;
    }

    static double vertical(int dy) { return dy > 0 ? dy * 6.0 : -dy * 1.5; }

    /** Stand exactly on this block. */
    record Block(int x, int y, int z) implements Goal {
        public Block(BlockPos p) { this(p.getX(), p.getY(), p.getZ()); }
        public boolean isGoal(int px, int py, int pz) { return px == x && py == y && pz == z; }
        public double heuristic(int px, int py, int pz) { return horizontal(x - px, z - pz) + vertical(y - py); }
        public String describe() { return x + " " + y + " " + z; }
    }

    /** Reach a column at any height. */
    record XZ(int x, int z) implements Goal {
        public boolean isGoal(int px, int py, int pz) { return px == x && pz == z; }
        public double heuristic(int px, int py, int pz) { return horizontal(x - px, z - pz); }
        public String describe() { return x + " ~ " + z; }
    }

    /** Get within radius of a point (feet block distance). */
    record Near(int x, int y, int z, double radius) implements Goal {
        public Near(BlockPos p, double r) { this(p.getX(), p.getY(), p.getZ(), r); }
        public boolean isGoal(int px, int py, int pz) {
            double dx = px - x, dy = py - y, dz = pz - z;
            return dx * dx + dy * dy + dz * dz <= radius * radius;
        }
        public double heuristic(int px, int py, int pz) {
            double h = horizontal(x - px, z - pz) + vertical(y - py);
            return Math.max(0, h - radius * WALK);
        }
        public String describe() { return "near " + x + " " + y + " " + z; }
    }

    /** Stand somewhere from which the block can be mined: next to it, above or below it, not inside it. */
    record Reach(int x, int y, int z) implements Goal {
        public Reach(BlockPos p) { this(p.getX(), p.getY(), p.getZ()); }
        public boolean isGoal(int px, int py, int pz) {
            int dx = Math.abs(px - x), dz = Math.abs(pz - z), dy = py - y;
            if (px == x && pz == z) return dy == 1 || dy == -2; // standing on it / under it (head next to it)
            return dx <= 1 && dz <= 1 && (dx + dz) == 1 && dy >= -1 && dy <= 0;
        }
        public double heuristic(int px, int py, int pz) {
            return Math.max(0, horizontal(x - px, z - pz) + vertical(y - py) - WALK);
        }
        public String describe() { return "block " + x + " " + y + " " + z; }
    }

    /** Any of several goals (closest wins). */
    record Any(List<Goal> goals) implements Goal {
        public boolean isGoal(int px, int py, int pz) {
            for (Goal g : goals) if (g.isGoal(px, py, pz)) return true;
            return false;
        }
        public double heuristic(int px, int py, int pz) {
            double best = Double.MAX_VALUE;
            for (Goal g : goals) best = Math.min(best, g.heuristic(px, py, pz));
            return best;
        }
        public String describe() { return goals.size() + " targets"; }
    }
}
