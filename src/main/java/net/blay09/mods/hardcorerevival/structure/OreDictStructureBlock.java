package net.blay09.mods.hardcorerevival.structure;

import cpw.mods.fml.common.registry.GameRegistry;
import net.blay09.mods.hardcorerevival.HardcoreRevival;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import java.util.List;

public class OreDictStructureBlock extends StructureBlock {

    private final String oreName;

    public OreDictStructureBlock(String oreName) {
        this.oreName = oreName;
    }

    @Override
    public boolean passes(World world, int x, int y, int z, Block block, int metadata) {
        int[] oreIDs = OreDictionary.getOreIDs(new ItemStack(block));
        for(int i : oreIDs) {
            if(OreDictionary.getOreName(i).equals(oreName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void printError(World world, int x, int y, int z, EntityPlayer entityPlayer) {
        StringBuilder sb = new StringBuilder("Expected ore dict name ");
        sb.append(oreName);
        sb.append(" but got ");
        Block block = world.getBlock(x, y, z);
        GameRegistry.UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(block);
        if(identifier != null) {
            sb.append(identifier.modId).append(":").append(identifier.name);
        } else {
            sb.append(block.getUnlocalizedName());
        }
        sb.append(":").append(world.getBlockMetadata(x, y, z));
        int[] oreIDs = OreDictionary.getOreIDs(new ItemStack(block));
        if(oreIDs.length > 0) {
            sb.append(" (");
            for(int i = 0; i < oreIDs.length; i++) {
                if(i > 0) {
                    sb.append(", ");
                }
                sb.append(OreDictionary.getOreName(oreIDs[i]));
            }
            sb.append(")");
        } else {
            sb.append(" (no ore dict names)");
        }
        sb.append(" at ").append(x).append(", ").append(y).append(", ").append(z);
        entityPlayer.addChatMessage(new ChatComponentText(sb.toString()));
    }

    @Override
    public void spawnBlock(World world, int x, int y, int z) {
        List<ItemStack> itemStacks = OreDictionary.getOres(oreName, false);
        if(!itemStacks.isEmpty()) {
            ItemStack itemStack = itemStacks.get(0);
            Item item = itemStack.getItem();
            if(item instanceof ItemBlock) {
                ItemBlock itemBlock = (ItemBlock) item;
                world.setBlock(x, y, z, itemBlock.field_150939_a, itemBlock.getMetadata(itemStack.getItemDamage()), 1 | 2);
                return;
            }
        }
        HardcoreRevival.logger.error("Failed to spawn ore dictionary block by name {}", oreName);

    }
}
