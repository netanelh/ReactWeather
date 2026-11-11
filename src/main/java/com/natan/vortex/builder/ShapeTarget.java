package com.natan.vortex.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** A shaped fill/clear inside the selection box: solid, walls, shell, hollow or sphere. */
public final class ShapeTarget implements BuildTarget {
    public enum Shape { SOLID, WALLS, SHELL, SPHERE }

    private final BlockState block; // null = clear/air
    private final List<BlockPos> cells = new ArrayList<>();
    private final String label;

    public ShapeTarget(BlockPos a, BlockPos b, BlockState block, Shape shape, String label) {
        this.block = block; this.label = label;
        int x1 = Math.min(a.getX(), b.getX()), x2 = Math.max(a.getX(), b.getX());
        int y1 = Math.min(a.getY(), b.getY()), y2 = Math.max(a.getY(), b.getY());
        int z1 = Math.min(a.getZ(), b.getZ()), z2 = Math.max(a.getZ(), b.getZ());
        double cx = (x1 + x2) / 2.0, cy = (y1 + y2) / 2.0, cz = (z1 + z2) / 2.0;
        double rx = (x2 - x1) / 2.0 + 0.5, ry = (y2 - y1) / 2.0 + 0.5, rz = (z2 - z1) / 2.0 + 0.5;
        for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) for (int x = x1; x <= x2; x++) {
            boolean edgeX = x == x1 || x == x2, edgeY = y == y1 || y == y2, edgeZ = z == z1 || z == z2;
            boolean keep = switch (shape) {
                case SOLID -> true;
                case WALLS -> edgeX || edgeZ;                 // 4 vertical sides, open top/bottom
                case SHELL -> edgeX || edgeY || edgeZ;        // all 6 faces
                case SPHERE -> {
                    double dx = (x - cx) / rx, dy = (y - cy) / ry, dz = (z - cz) / rz;
                    yield dx * dx + dy * dy + dz * dz <= 1.0;
                }
            };
            if (keep) cells.add(new BlockPos(x, y, z));
        }
    }

    @Override public List<BlockPos> cells() { return cells; }
    @Override public BlockState desired(BlockPos w) { return block; }
    @Override public String label() { return label; }
}
