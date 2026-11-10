package com.society.sunlitindustry.performance.gt;

import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.society.sunlitindustry.mixin.performance.gt.GtPreviewBlockInfoAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;

/** Captures data, never BlockInfo.lastEntity or callbacks which could own a world. */
public final class GtShapeSnapshot {
    private final List<Cell[][][]> shapes;

    private GtShapeSnapshot(List<Cell[][][]> shapes) { this.shapes = shapes; }

    public static GtShapeSnapshot capture(List<MultiblockShapeInfo> source) {
        if (source.size() > 32) return null;
        var snapshots = new ArrayList<Cell[][][]>(source.size());
        int count = 0;
        for (var shape : source) {
            var blocks = shape.getBlocks();
            var cells = new Cell[blocks.length][][];
            for (int z = 0; z < blocks.length; z++) {
                cells[z] = new Cell[blocks[z].length][];
                for (int y = 0; y < blocks[z].length; y++) {
                    if ((count += blocks[z][y].length) > 65_536) return null;
                    cells[z][y] = new Cell[blocks[z][y].length];
                    for (int x = 0; x < blocks[z][y].length; x++) {
                        var info = blocks[z][y][x];
                        if (info == null) continue;
                        if (info.getClass() != BlockInfo.class) return null;
                        var access = (GtPreviewBlockInfoAccess) info;
                        if (access.sunlitindustry$postCreate() != null) return null;
                        var tag = access.sunlitindustry$tag();
                        var stack = access.sunlitindustry$itemStack();
                        cells[z][y][x] = new Cell(info.getBlockState(), info.hasBlockEntity(),
                                tag == null ? null : tag.copy(),
                                stack == null ? null : stack.save(new CompoundTag()));
                    }
                }
            }
            snapshots.add(cells);
        }
        return new GtShapeSnapshot(snapshots);
    }

    public List<MultiblockShapeInfo> copy() {
        var result = new ArrayList<MultiblockShapeInfo>(shapes.size());
        for (var cells : shapes) {
            var blocks = new BlockInfo[cells.length][][];
            for (int z = 0; z < cells.length; z++) {
                blocks[z] = new BlockInfo[cells[z].length][];
                for (int y = 0; y < cells[z].length; y++) {
                    blocks[z][y] = new BlockInfo[cells[z][y].length];
                    for (int x = 0; x < cells[z][y].length; x++) {
                        var cell = cells[z][y][x];
                        if (cell == null) continue;
                        var info = new BlockInfo(cell.state, cell.hasBlockEntity,
                                cell.stack == null ? null : ItemStack.of(cell.stack.copy()), null);
                        if (cell.tag != null) info.setTag(cell.tag.copy());
                        blocks[z][y][x] = info;
                    }
                }
            }
            result.add(new MultiblockShapeInfo(blocks));
        }
        return result;
    }

    private record Cell(BlockState state, boolean hasBlockEntity, CompoundTag tag, CompoundTag stack) {}
}
