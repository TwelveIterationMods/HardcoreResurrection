package net.blay09.mods.hardcorerevival.structure;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

public abstract class StructureBlock {
    public abstract boolean passes(World world, int x, int y, int z, Block block, int metadata);
    public abstract void printError(World world, int x, int y, int z, EntityPlayer entityPlayer);
    public abstract void spawnBlock(World world, int x, int y, int z);
}

