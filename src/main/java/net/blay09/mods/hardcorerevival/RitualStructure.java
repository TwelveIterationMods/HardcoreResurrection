package net.blay09.mods.hardcorerevival;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

public class RitualStructure {

    public static class RitualException extends RuntimeException {
        public RitualException(String message) {
            super(message);
        }
    }

    public String getActivationItemHelpText() {
        return activationItemHelpText;
    }

    public String[] getStructureHelpText() {
        return structureHelpText;
    }

    public static class StructureBlock {
        public final Block block;
        public final int metadata;

        public StructureBlock(Block block, int metadata) {
            this.block = block;
            this.metadata = metadata;
        }
    }

    private final ItemStack activationItemStack;
    private final boolean consumeActivationItem;
    private int headX;
    private int headY;
    private int headZ;
    private StructureBlock[][][] structureMap;
    private boolean[][][] consumeMap;
    private String[] structureHelpText;
    private String activationItemHelpText;

    public RitualStructure(JsonObject object) {
        JsonObject mapping = object.getAsJsonObject("mapping");
        final Map<Character, StructureBlock> blockMapping = new HashMap<>();
        blockMapping.put('H', new StructureBlock(Blocks.skull, -1));
        for(Map.Entry<String, JsonElement> entry : mapping.entrySet()) {
            if(entry.getKey().length() != 1) {
                throw new RitualException("Configured hardcore revival ritual activation structure is invalid: mapping keys need to be one character long");
            }
            char c = Character.toUpperCase(entry.getKey().charAt(0));
            if(c == 'H') {
                throw new RitualException("Configured hardcore revival ritual activation structure is invalid: mapping 'H' is reserved for the player head");
            }
            if(blockMapping.containsKey(c)) {
                throw new RitualException("Configured hardcore revival ritual activation structure is invalid: mapping '" + entry.getKey() + "' is alerady defined");
            }
            Matcher matcher = HardcoreRevival.BLOCK_PATTERN.matcher(entry.getValue().getAsString());
            if(matcher.find()) {
                String blockName = matcher.group(1);
                Block block = (Block) Block.blockRegistry.getObject(blockName);
                if(block == null) {
                    throw new RitualException("Configured hardcore revival ritual activation structure is invalid: block " + blockName + " can not be found");
                }
                String metadata = matcher.group(2);
                blockMapping.put(c, new StructureBlock(block, metadata != null ? Integer.parseInt(matcher.group(2)) : 0));
            } else {
                throw new RitualException("Configured hardcore revival ritual mappings are invalid: incorrect block format for " + entry.getValue().getAsString());
            }
        }
        boolean structureContainsHead = false;
        JsonArray structureY = object.getAsJsonArray("structure");
        int structureHeight = structureY.size();
        for(int y = 0; y < structureY.size(); y++) {
            JsonArray structureZ = structureY.get(y).getAsJsonArray();
            for(int z = 0; z < structureZ.size(); z++) {
                String structureX = structureZ.get(z).getAsString();
                for(int x = 0; x < structureX.length(); x++) {
                    if(structureMap == null) {
                        structureMap = new StructureBlock[structureX.length()][structureY.size()][structureZ.size()];
                    } else {
                        if(structureMap.length != structureX.length() || structureMap[x].length != structureY.size()) {
                            throw new RitualException("Configured hardcore revival ritual structure is invalid: structure layers are of different sizes");
                        }
                    }
                    char c = Character.toUpperCase(structureX.charAt(x));
                    if(c == 'H') {
                        if(structureContainsHead) {
                            throw new RitualException("Configured hardcore revival ritual structure is invalid: only one player head in the ritual allowed");
                        }
                        headX = x;
                        headY = structureHeight - y - 1;
                        headZ = z;
                        structureContainsHead = true;
                    }
                    StructureBlock block = blockMapping.get(c);
                    if(block == null) {
                        throw new RitualException("Configured hardcore revival ritual structure is invalid: mapping '" + c + "' is not defined");
                    }
                    structureMap[x][structureHeight - y - 1][z] = block;
                }
            }
        }
        if(!structureContainsHead) {
            throw new RitualException("Configured hardcore revival ritual structure is invalid: no player head in structure (mapping H)");
        }
        JsonArray consumeStructureY = object.getAsJsonArray("consumeStructure");
        if(structureY.size() != consumeStructureY.size()) {
            throw new RitualException("Configured hardcore revival ritual structure is invalid: structure and consumeStructure are of different sizes");
        }
        for(int y = 0; y < consumeStructureY.size(); y++) {
            JsonArray consumeStructureZ = consumeStructureY.get(y).getAsJsonArray();
            for(int z = 0; z < consumeStructureZ.size(); z++) {
                String consumeStructureX = consumeStructureZ.get(z).getAsString();
                for(int x = 0; x < consumeStructureX.length(); x++) {
                    if(consumeMap == null) {
                        consumeMap = new boolean[consumeStructureX.length()][consumeStructureY.size()][consumeStructureZ.size()];
                    } else {
                        if(consumeMap.length != consumeStructureX.length() || consumeMap[x].length != consumeStructureY.size()) {
                            throw new RitualException("Configured hardcore revival ritual structure is invalid: structure layers are of different sizes");
                        }
                    }
                    consumeMap[x][structureHeight - y - 1][z] = consumeStructureX.charAt(x) == '1';
                    if(x == headX && structureHeight - y - 1 == headY && z == headZ && !consumeMap[x][structureHeight - y - 1][z]) {
                        throw new RitualException("Configured hardcore revival ritual structure is invalid: head always needs to be consumed");
                    }
                }
            }
        }

        String activationItem = object.get("activationItem").getAsString();
        Matcher matcher = HardcoreRevival.ITEM_STACK_PATTERN.matcher(activationItem);
        if(matcher.find()) {
            Item item = (Item) Item.itemRegistry.getObject(matcher.group(2));
            if(item == null) {
                throw new RitualException("Configured hardcore revival ritual activation item is invalid: item " + matcher.group(2) + " can not be found");
            }
            String stackSize = matcher.group(1);
            String metadata = matcher.group(3);
            activationItemStack = new ItemStack(item, stackSize != null ? Integer.parseInt(stackSize) : 1, metadata != null ? Integer.parseInt(metadata) : 0);
        } else {
            throw new RitualException("Configured hardcore revival ritual activation item is invalid: incorrect format");
        }
        if(object.has("activationItemNBT")) {
            try {
                String activationItemNBT = object.get("activationItemNBT").getAsString();
                NBTBase tagCompound = JsonToNBT.func_150315_a(activationItemNBT);
                if (tagCompound.getId() != Constants.NBT.TAG_COMPOUND) {
                    activationItemStack.setTagCompound((NBTTagCompound) tagCompound);
                }
            } catch (NBTException e) {
                throw new RitualException("Configured hardcore revival ritual activation item is invalid: incorrect nbt data format");
            }
        }
        consumeActivationItem = !object.has("consumeActivationItem") || object.get(" consumeActivationItem").getAsBoolean();

        if(object.has("structureHelpText")) {
            JsonArray structureHelp = object.get("structureHelpText").getAsJsonArray();
            structureHelpText = new String[structureHelp.size()];
            for (int i = 0; i < structureHelpText.length; i++) {
                structureHelpText[i] = structureHelp.get(i).getAsString();
                structureHelpText[i] = structureHelpText[i].replace("\r", "");
            }
        } else {
            structureHelpText = new String[0];
        }
        if(object.has("activationItemHelpText")) {
            activationItemHelpText = object.get("activationItemHelpText").getAsString();
        } else {
            activationItemHelpText = activationItemStack.getDisplayName();
        }
    }

    public boolean checkStructure(World world, int x, int y, int z) {
        int startX = x - headX;
        int startY = y - headY;
        int startZ = z - headZ;
        for(int i = 0; i < structureMap.length; i++) {
            for(int j = 0; j < structureMap[i].length; j++) {
                for(int k = 0; k < structureMap[i][j].length; k++) {
                    Block block = world.getBlock(startX + i, startY + j, startZ + k);
                    int metadata = world.getBlockMetadata(startX + i, startY + j, startZ + k);
                    StructureBlock structureBlock = structureMap[i][j][k];
                    if(structureBlock.block != block || (structureBlock.metadata != -1 && structureBlock.metadata != metadata)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void consumeStructure(World world, int x, int y, int z) {
        int startX = x - headX;
        int startY = y - headY;
        int startZ = z - headZ;
        for(int i = 0; i < structureMap.length; i++) {
            for(int j = 0; j < structureMap[i].length; j++) {
                for(int k = 0; k < structureMap[i][j].length; k++) {
                    Block block = world.getBlock(startX + i, startY + j, startZ + k);
                    int metadata = world.getBlockMetadata(startX + i, startY + j, startZ + k);
                    StructureBlock structureBlock = structureMap[i][j][k];
                    if(structureBlock.block == block && (structureBlock.metadata == -1 || structureBlock.metadata == metadata)) {
                        if(consumeMap[i][j][k]) {
                            world.setBlockToAir(startX + i, startY + j, startZ + k);
                        }
                    }
                }
            }
        }
    }

    public boolean checkActivationItem(ItemStack itemStack) {
        return itemStack != null && activationItemStack.isItemEqual(itemStack) && ItemStack.areItemStackTagsEqual(itemStack, activationItemStack);
    }

    public void consumeActivationItem(ItemStack itemStack) {
        if(consumeActivationItem) {
            itemStack.stackSize -= activationItemStack.stackSize;
        }
    }

    public int getHeadY() {
        return headY;
    }

}

