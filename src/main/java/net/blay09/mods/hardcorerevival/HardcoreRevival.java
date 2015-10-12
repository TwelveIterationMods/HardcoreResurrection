package net.blay09.mods.hardcorerevival;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.UserListBans;
import net.minecraft.server.management.UserListBansEntry;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;

@Mod(modid = "HardcoreRevival", name = "Hardcore Revival", acceptableRemoteVersions = "*")
public class HardcoreRevival {

    public static final String HARDCORE_DEATH_BAN_REASON = "Death in Hardcore";

    @Mod.Instance
    public static HardcoreRevival instance;

    @SidedProxy(clientSide = "net.blay09.mods.hardcorerevival.CommonProxy", serverSide = "net.blay09.mods.hardcorerevival.ServerProxy")
    public static CommonProxy proxy;

    public static boolean enableHardcoreRevival;
    public static boolean enableFireworks;
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

    public static String helpBookText;
    public static RitualStructure ritualStructure;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        enableHardcoreRevival = config.getBoolean("enableHardcoreRevival", "general", true, "A convenience option to disable the entirety of the mod, for example as a default in modpacks.");
        if(!enableHardcoreRevival) {
            return;
        }
        enableFireworks = config.getBoolean("enableFireworks", "general", true, "Should fireworks be fired from corpses to help other players find them?");
        fireworksInterval = config.getInt("fireworksInterval", "general", 480, 200, 2400, "The interval at which fireworks are being fired from corpses, in ticks (1 second = 20 ticks)");
        enableSillyThings = config.getBoolean("enableSillyThings", "general", false, "Should silly things be enabled, such as the special revival method for GregTheCart?");
        healthOnRespawn = config.getInt("healthOnRespawn", "general", 10, 1, 20, "How much health should respawned players start with?");
        foodLevelOnRespawn = config.getInt("foodLevelOnRespawn", "general", 0, 0, 20, "How much food points should respawned players start with?");
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
        config.save();

        File hardcoreRevivalDir = new File(event.getModConfigurationDirectory(), "HardcoreRevival/");
        if(hardcoreRevivalDir.exists() || hardcoreRevivalDir.mkdirs()) {
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
            extractStructure(hardcoreRevivalDir, "Goldman");
            Gson gson = new Gson();
            try {
                JsonObject object = gson.fromJson(new FileReader(new File(hardcoreRevivalDir, ritualStructureName + ".json")), JsonObject.class);
                ritualStructure = new RitualStructure(object);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
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
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
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
        bookText = bookText.replace("<ActivationItem>", ritualStructure.getActivationItemHelpText());
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
        String[] structureHelpText = ritualStructure.getStructureHelpText();
        for (String pageText : pageTexts) {
            if(structureHelpText.length > 0) {
                int foundStructure = pageText.indexOf("<Structure>");
                if(foundStructure != -1) {
                    pageText = pageText.replace("<Structure>", structureHelpText[0]);
                    pages.appendTag(new NBTTagString(pageText));
                    for(int i = 1; i < structureHelpText.length; i++) {
                        pages.appendTag(new NBTTagString(structureHelpText[i]));
                    }
                    continue;
                }
            }
            pages.appendTag(new NBTTagString(pageText));
        }
        tagCompound.setTag("pages", pages);
        itemStack.setTagCompound(tagCompound);
        return itemStack;
    }

    public static EntityPlayerMP revivePlayer(String playerName, World world, int x, int y, int z) {
        return revivePlayer(MinecraftServer.getServer().getConfigurationManager().func_152612_a(playerName), world, x, y, z);
    }

    public static EntityPlayerMP revivePlayer(EntityPlayerMP entityPlayer, World world, int x, int y, int z) {
        if (entityPlayer != null && entityPlayer.getHealth() <= 0f && unbanHardcoreDeath(entityPlayer.getGameProfile())) {
            EntityPlayerMP respawnPlayer = MinecraftServer.getServer().getConfigurationManager().respawnPlayer(entityPlayer, 0, false);
            respawnPlayer.travelToDimension(world.provider.dimensionId);
            respawnPlayer.playerNetServerHandler.setPlayerLocation(x, y, z, entityPlayer.rotationYaw, entityPlayer.rotationPitch);
            entityPlayer.playerNetServerHandler.playerEntity = respawnPlayer;
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


}
