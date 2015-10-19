package net.blay09.mods.hardcorerevival;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.blay09.mods.hardcorerevival.structure.RitualException;
import net.blay09.mods.hardcorerevival.structure.RitualStructure;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.UserListBans;
import net.minecraft.server.management.UserListBansEntry;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod(modid = "HardcoreRevival", name = "Hardcore Revival", acceptableRemoteVersions = "*")
public class HardcoreRevival {

    public static final String HARDCORE_DEATH_BAN_REASON = "Death in Hardcore";
    public static final Pattern ITEM_STACK_PATTERN = Pattern.compile("(?:([0-9]+)x)?([\\w:]+)(?:[@:]([0-9]+))?");
    public static final Pattern BLOCK_PATTERN = Pattern.compile("([\\w:]+)(?:[@:]([0-9]+))?");

    public static final Logger logger = LogManager.getLogger();

    @Mod.Instance
    public static HardcoreRevival instance;

    @SidedProxy(clientSide = "net.blay09.mods.hardcorerevival.CommonProxy", serverSide = "net.blay09.mods.hardcorerevival.ServerProxy")
    public static CommonProxy proxy;

    public static boolean enableHardcoreRevival;
    public static boolean enableDeathFireworks;
    public static boolean enableCorpseLocating;
    public static int fireworksInterval;
    public static int healthOnRespawn;
    public static int foodLevelOnRespawn;
    public static float saturationOnRespawn;
    public static ConfiguredPotionEffect[] effectsOnRespawn;
    public static int damageOnRitual;
    public static ConfiguredPotionEffect[] effectsOnRitual;
    public static int experienceCost;
    public static String ritualStructureName;
    public static boolean enableSillyThings;
    public static ItemStack locatorItem;
    public static boolean disallowFakePlayers;

    public static String helpBookText;
    public static RitualStructure ritualStructure;
    public static RitualException ritualStructureError;

    private File configDir;
    private File configFile;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configDir = event.getModConfigurationDirectory();
        configFile = event.getSuggestedConfigurationFile();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        loadConfig();
        proxy.postInit(event);
    }

    public void loadConfig() {
        Configuration config = new Configuration(configFile);
        enableHardcoreRevival = config.getBoolean("enableHardcoreRevival", "general", true, "A convenience option to disable the entirety of the mod, for example as a default in modpacks.");
        if(!enableHardcoreRevival) {
            return;
        }
//        enableFireworks = config.getBoolean("enableFireworks", "general", true, "Should fireworks be fired from corpses to help other players find them?");
        enableDeathFireworks = config.getBoolean("enableDeathFireworks", "general", false, "Don't even bother setting this to true, clients freeze when fireworks are launched while they're dead (Vanilla bug).");
        enableDeathFireworks = false;
        fireworksInterval = config.getInt("fireworksInterval", "general", 480, 200, 2400, "The interval at which fireworks are being fired from corpses, in ticks (1 second = 20 ticks)");
        enableSillyThings = config.getBoolean("enableSillyThings", "general", false, "Should silly things be enabled, such as the special revival method for GregTheCart?");
        healthOnRespawn = config.getInt("healthOnRespawn", "general", 10, 1, 20, "How much health should respawned players start with?");
        foodLevelOnRespawn = config.getInt("foodLevelOnRespawn", "general", 1, 0, 20, "How much food points should respawned players start with?");
        saturationOnRespawn = config.getInt("saturationOnRespawn", "general", 0, 0, 20, "How much saturation should respawned players start with?");
        experienceCost = config.getInt("experienceCost", "general", 5, 0, 100, "How much experience levels should reviving players cost?");
        String[] cfgEffectsOnRespawn = config.getStringList("effectsOnRespawn", "general", new String[] { "600xpotion.blindness", "1200xpotion.weakness@2" }, "What potion effects should be applied to respawned players? Format: <TimeInTicks>x<PotionNameOrID>@<PotionLevel>");
        effectsOnRespawn = new ConfiguredPotionEffect[cfgEffectsOnRespawn.length];
        for(int i = 0; i < cfgEffectsOnRespawn.length; i++) {
            effectsOnRespawn[i] = new ConfiguredPotionEffect(cfgEffectsOnRespawn[i]);
        }
        String[] cfgEffectsOnRitual = config.getStringList("effectsOnRitual", "general", new String[] { "300xpotion.hunger"}, "What potion effects should be applied to players performing the ritual? Format: <TimeInTicks>x<PotionNameOrID>@<PotionLevel>");
        damageOnRitual = config.getInt("damageOnRitual", "general", 10, 0, 20, "How much damage should players performing the ritual take?");
        effectsOnRitual = new ConfiguredPotionEffect[cfgEffectsOnRitual.length];
        for(int i = 0; i < cfgEffectsOnRitual.length; i++) {
            effectsOnRitual[i] = new ConfiguredPotionEffect(cfgEffectsOnRitual[i]);
        }
        ritualStructureName = config.getString("ritualStructure", "general", "Goldman", "Which ritual structure should be used? See config/HardcoreRevival - you can name any .json file from that folder (you can also create your own there)");
        enableCorpseLocating = config.getBoolean("enableCorpseLocating", "general", true, "Should players be able to locate corpses by burning the configured locator item (paper by default) named as the dead player?");
        disallowFakePlayers = config.getBoolean("disallowFakePlayers", "general", true, "Should fake players be disallowed from performing the ritual?");
        String locatorItemName = config.getString("locatorItem", "general", "minecraft:paper", "The item that can be named after a dead player and burned in order to get an indication to where the corpse is located. Format: \"modid:item@metadata\"");
        Matcher matcher = ITEM_STACK_PATTERN.matcher(locatorItemName);
        if(matcher.find()) {
            String itemName = matcher.group(2);
            String metadata = matcher.group(3);
            Item item = (Item) Item.itemRegistry.getObject(itemName);
            if(item != null) {
                locatorItem = new ItemStack(item, 1, metadata != null ? Integer.parseInt(metadata) : 0);
            }
        }
        if(locatorItem == null) {
            logger.error("Invalid format for locatorItem property; falling back to paper");
            locatorItem = new ItemStack(Items.paper);
        }
        config.save();

        File hardcoreRevivalDir = new File(configDir, "HardcoreRevival/");
        if(hardcoreRevivalDir.exists() || hardcoreRevivalDir.mkdirs()) {
            // Load the help book
            File helpBookFile = new File(hardcoreRevivalDir, "HardcoreRevival-HelpBook.txt");
            if (helpBookFile.exists()) {
                try {
                    helpBookText = FileUtils.readFileToString(helpBookFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    helpBookText = IOUtils.toString(getClass().getResourceAsStream("/assets/HardcoreRevival/HelpBook.txt"));
                    FileUtils.writeStringToFile(helpBookFile, helpBookText);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Extract default rituals
            extractStructure(hardcoreRevivalDir, "Goldman");

            // Load the ritual
            ritualStructureError = null;
            Gson gson = new Gson();
            try {
                JsonObject object = gson.fromJson(new FileReader(new File(hardcoreRevivalDir, ritualStructureName + ".json")), JsonObject.class);
                ritualStructure = new RitualStructure(object);
            } catch (FileNotFoundException e) {
                ritualStructureError = new RitualException("The ritual file " + ritualStructureName + " could not be found");
            } catch (RitualException e) {
                ritualStructureError = e;
            }
            if(ritualStructureError != null) {
                logger.error(ritualStructureError.getMessage());
            }
        }
        if(helpBookText == null) {
            helpBookText = "Hardcore Revival failed to load the book text. Oops";
        } else {
            helpBookText = helpBookText.replace("\r", "");
            helpBookText = helpBookText.replaceAll("<PageBreak>\n\n?", "<PageBreak>");
        }
    }

    private void extractStructure(File configDir, String name) {
        File file = new File(configDir, name + ".json");
        if(!file.exists()) {
            try {
                FileUtils.writeStringToFile(file, IOUtils.toString(getClass().getResourceAsStream("/assets/HardcoreRevival/" + name + ".json")));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandHardcoreRevival());
    }

    public static ItemStack getHelpBook(String deadPerson) {
        ItemStack itemStack = new ItemStack(Items.written_book);
        NBTTagCompound tagCompound = new NBTTagCompound();
        tagCompound.setString("title", "\u00a7eHardcore Revival");
        tagCompound.setString("author", "BlayTheNinth");
        NBTTagList pages = new NBTTagList();
        String bookText = HardcoreRevival.helpBookText;
        bookText = bookText.replace("<DeadPerson>", deadPerson);
        if(ritualStructure != null) {
            bookText = bookText.replace("<ActivationItem>", ritualStructure.getActivationItemHelpText());
        }
        bookText = bookText.replace("<LocatorItem>", locatorItem.getDisplayName());
        StringBuilder ritualNotes = new StringBuilder();
        if(experienceCost > 0) {
            ritualNotes.append("- Requires ").append(experienceCost).append(" experience levels\n");
        }
        if(damageOnRitual > 0) {
            ritualNotes.append("- Requires ").append(damageOnRitual / 2f).append(" hearts\n");
        }
        if(effectsOnRespawn.length > 0 || effectsOnRitual.length > 0) {
            ritualNotes.append("- Unexpected side effects may occur\n");
        }
        if(ritualNotes.length() == 0) {
            ritualNotes.append("- Not much to say, really\n");
        }
        bookText = bookText.replace("<RitualNotes>", ritualNotes.toString());
        String[] pageTexts = bookText.split("<PageBreak>");
        if(ritualStructure != null) {
            String[] structureHelpText = ritualStructure.getStructureHelpText();
            for (String pageText : pageTexts) {
                if (structureHelpText.length > 0) {
                    int foundStructure = pageText.indexOf("<Structure>");
                    if (foundStructure != -1) {
                        pageText = pageText.replace("<Structure>", structureHelpText[0]);
                        pages.appendTag(new NBTTagString(pageText));
                        for (int i = 1; i < structureHelpText.length; i++) {
                            pages.appendTag(new NBTTagString(structureHelpText[i]));
                        }
                        continue;
                    }
                }
                pages.appendTag(new NBTTagString(pageText));
            }
        }
        tagCompound.setTag("pages", pages);
        itemStack.setTagCompound(tagCompound);
        return itemStack;
    }

    public static EntityPlayerMP revivePlayer(EntityPlayerMP entityPlayer, World world, int x, int y, int z) {
        if (entityPlayer != null && entityPlayer.getHealth() <= 0f && unbanHardcoreDeath(entityPlayer.getGameProfile())) {
            entityPlayer.setSpawnChunk(new ChunkCoordinates(x, y, z), true, world.provider.dimensionId);
            EntityPlayerMP respawnPlayer = MinecraftServer.getServer().getConfigurationManager().respawnPlayer(entityPlayer, 0, false);
            if(respawnPlayer.dimension != world.provider.dimensionId) {
                respawnPlayer.travelToDimension(world.provider.dimensionId);
            }
            entityPlayer.playerNetServerHandler.playerEntity = respawnPlayer;
            respawnPlayer.setPositionAndUpdate(x, y, z);
            respawnPlayer.setHealth(HardcoreRevival.healthOnRespawn);
            respawnPlayer.getFoodStats().foodLevel = HardcoreRevival.foodLevelOnRespawn;
            respawnPlayer.getFoodStats().foodSaturationLevel = HardcoreRevival.saturationOnRespawn;
            for (ConfiguredPotionEffect potionEffect : HardcoreRevival.effectsOnRespawn) {
                respawnPlayer.addPotionEffect(new PotionEffect(potionEffect.potion.getId(), potionEffect.timeInTicks, potionEffect.potionLevel));
            }
            return respawnPlayer;
        }
        return null;
    }

    public static boolean unbanHardcoreDeath(GameProfile gameProfile) {
        UserListBans banList = MinecraftServer.getServer().getConfigurationManager().func_152608_h();
        UserListBansEntry entry = (UserListBansEntry) banList.func_152683_b(gameProfile);
        if (entry == null) {
            return true;
        }
        if (entry.getBanReason().equals(HARDCORE_DEATH_BAN_REASON)) {
            banList.func_152684_c(gameProfile);
            return true;
        }
        return false;
    }

    public static NBTTagCompound getHardcoreRevivalData(EntityPlayer entityPlayer) {
        NBTTagCompound tagCompound = entityPlayer.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        NBTTagCompound hcrCompound = tagCompound.getCompoundTag("HardcoreRevival");
        tagCompound.setTag("HardcoreRevival", hcrCompound);
        entityPlayer.getEntityData().setTag(EntityPlayer.PERSISTED_NBT_TAG, tagCompound);
        return hcrCompound;
    }


    public static ItemStack getPlayerHead(EntityPlayer entityPlayer) {
        ItemStack itemStack = new ItemStack(Items.skull, 1, 3);
        NBTTagCompound tagCompound = new NBTTagCompound();
        NBTTagCompound ownerCompound = new NBTTagCompound();
        NBTUtil.func_152460_a(ownerCompound, entityPlayer.getGameProfile());
        tagCompound.setTag("SkullOwner", ownerCompound);
        itemStack.setTagCompound(tagCompound);
        return itemStack;
    }

    public static void spawnPlayerGrave(World world, int x, int y, int z, EntityPlayer entityPlayer) {
        for(int yOff = 2; y - yOff > 5; yOff++) {
            if(isValidGraveBlock(world, x, y - yOff, z)) {
                if(isValidGraveBlock(world, x, y - yOff + 1, z)) {
                    world.setBlock(x, y - 1, z, Blocks.dirt);
                }
                if(isValidGraveFlowerBlock(world, x, y - yOff + 2, z)) {
                    world.setBlock(x, y, z, Blocks.red_flower, 4, 2);
                }
                spawnPlayerHead(world, x, y - yOff, z, entityPlayer);
                return;
            }
        }
        // For some reason, there was no valid block in the whole area; let's be less merciful and replace anything but tile entities
        for(int yOff = 2; y - yOff > 5; yOff++) {
            if(world.getTileEntity(x, y - yOff, z) == null) {
                if(isValidGraveBlock(world, x, y - yOff + 1, z)) {
                    world.setBlock(x, y - 1, z, Blocks.dirt);
                }
                if(isValidGraveFlowerBlock(world, x, y - yOff + 2, z)) {
                    world.setBlock(x, y, z, Blocks.red_flower, 4, 2);
                }
                spawnPlayerHead(world, x, y - yOff, z, entityPlayer);
                return;
            }
        }
        // Screw it
        Block block = world.getBlock(x, y, z);
        int metadata = world.getBlockMetadata(x, y, z);
        block.breakBlock(world, x, y, z, block, metadata);
        spawnPlayerHead(world, x, y, z, entityPlayer);
    }

    private static boolean isValidGraveBlock(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return (block.isAir(world, x, y, z) || block == Blocks.cobblestone || block == Blocks.stone || block == Blocks.dirt || block == Blocks.grass || block == Blocks.gravel || block == Blocks.sand || block == Blocks.sandstone);
    }

    private static boolean isValidGraveFlowerBlock(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        return (block.isAir(world, x, y, z) || block == Blocks.red_flower || block == Blocks.yellow_flower || block == Blocks.grass || block == Blocks.tallgrass);
    }

    public static void spawnPlayerHead(World world, int x, int y, int z, EntityPlayer entityPlayer) {
        world.setBlock(x, y, z, Blocks.skull, 1, 2);
        TileEntitySkull skull = (TileEntitySkull) world.getTileEntity(x, y, z);
        skull.func_152106_a(entityPlayer.getGameProfile());
        world.markBlockForUpdate(x, y, z);
    }
}
