package net.blay09.mods.hardcorerevival.structure;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

public class DefaultStructureBlock extends StructureBlock {
    private final Block block;
    private final int metadata;

    public DefaultStructureBlock(Block block, int metadata) {
        this.block = block;
        this.metadata = metadata;
    }

    @Override
    public boolean passes(World world, int x, int y, int z, Block block, int metadata) {
        return this.block == Blocks.air && block.isAir(world, x, y, z) || !(this.block != block || (this.metadata != -1 && this.metadata != metadata));
    }

    @Override
    public void printError(World world, int x, int y, int z, EntityPlayer entityPlayer) {
        StringBuilder sb = new StringBuilder("Expected block ");
        GameRegistry.UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(block);
        if(identifier != null) {
            sb.append(identifier.modId).append(":").append(identifier.name);
        } else {
            sb.append(block.getUnlocalizedName());
        }
        sb.append(":").append(metadata == -1 ? "*" : metadata);
        sb.append(" but got ");
        Block block = world.getBlock(x, y, z);
        identifier = GameRegistry.findUniqueIdentifierFor(block);
        if(identifier != null) {
            sb.append(identifier.modId).append(":").append(identifier.name);
        } else {
            sb.append(block.getUnlocalizedName());
        }
        sb.append(":").append(world.getBlockMetadata(x, y, z));
        sb.append(" at ").append(x).append(", ").append(y).append(", ").append(z);
        entityPlayer.addChatMessage(new ChatComponentText(sb.toString()));
    }

    @Override
    public void spawnBlock(World world, int x, int y, int z) {
        int newMetadata = metadata != -1 ? metadata : 0;
        if(block == Blocks.skull) {
            newMetadata = 1;
        }
        world.setBlock(x, y, z, block, newMetadata, 1 | 2);
    }
}
