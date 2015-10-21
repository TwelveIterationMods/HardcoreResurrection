package net.blay09.mods.hardcorerevival.structure;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;

public class AnySolidStructureBlock extends StructureBlock {
    @Override
    public boolean passes(World world, int x, int y, int z, Block block, int metadata) {
        return block.isOpaqueCube();
    }

    @Override
    public void printError(World world, int x, int y, int z, EntityPlayer entityPlayer) {
        StringBuilder sb = new StringBuilder("Expected solid block but got ");
        Block block = world.getBlock(x, y, z);
        GameRegistry.UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(block);
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
        world.setBlock(x, y, z, Blocks.cobblestone);
    }
}
