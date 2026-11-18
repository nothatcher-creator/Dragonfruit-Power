package com.nothatcher.vehicleforge.block;

import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

final class Shapes {
    private Shapes() {}

    /** rotate a shape clockwise (seen from above) by quarter turns: north -> east -> south -> west */
    static VoxelShape rotate(VoxelShape shape, int quarters) {
        VoxelShape out = shape;
        for (int i = 0; i < quarters; i++) {
            VoxelShape[] acc = {VoxelShapes.empty()};
            out.forEachBox((x0, y0, z0, x1, y1, z1) ->
                    acc[0] = VoxelShapes.union(acc[0], VoxelShapes.cuboid(1 - z1, y0, x0, 1 - z0, y1, x1)));
            out = acc[0];
        }
        return out;
    }
}
