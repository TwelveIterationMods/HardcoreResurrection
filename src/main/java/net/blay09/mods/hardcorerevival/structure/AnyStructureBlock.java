package net.blay09.mods.hardcorerevival.structure;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public class AnyStructureBlock extends StructureBlock {
    @Override
    public boolean passes(World world, int x, int y, int z, Block block, int metadata) {
        return true;
    }

    @Override
    public void printError(World world, int x, int y, int z, EntityPlayer entityPlayer) {
    }

    @Override
    public void spawnBlock(World world, int x, int y, int z) {
    }
}
