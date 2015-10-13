package net.blay09.mods.hardcorerevival;

import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.UserListBans;
import net.minecraft.server.management.UserListBansEntry;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.minecart.MinecartUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;

import java.util.*;

@SuppressWarnings("unused")
public class ServerProxy extends CommonProxy {

    public static final Map<GameProfile, EntityPlayerMP> deadPlayers = new HashMap<>();

    private int tickTimer;

    public void init(FMLInitializationEvent event) {
        if (HardcoreRevival.enableHardcoreRevival) {
            FMLCommonHandler.instance().bus().register(this);
            MinecraftForge.EVENT_BUS.register(this);
        }
    }

    @SubscribeEvent
    public void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.player.getHealth() <= 0) {
            deadPlayers.put(event.player.getGameProfile(), (EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onPlayerLeft(PlayerEvent.PlayerLoggedOutEvent event) {
        deadPlayers.remove(event.player.getGameProfile());
        HardcoreRevival.unbanHardcoreDeath(event.player.getGameProfile());
    }

    @SubscribeEvent
    public void onBreakBlock(BlockEvent.BreakEvent event) {
        if (event.block == Blocks.skull) {
            TileEntitySkull skull = (TileEntitySkull) event.world.getTileEntity(event.x, event.y, event.z);
            if (skull.func_152108_a() != null && event.getPlayer() != null) {
                NBTTagCompound tagCompound = HardcoreRevival.getHardcoreRevivalData(event.getPlayer());
                if (!tagCompound.getBoolean("HelpBook")) {
                    ItemStack itemStack = HardcoreRevival.getHelpBook(skull.func_152108_a().getName());
                    if(!event.getPlayer().inventory.addItemStackToInventory(itemStack)) {
                        event.getPlayer().dropPlayerItemWithRandomChoice(itemStack, false);
                    }
                    tagCompound.setBoolean("HelpBook", true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.entity instanceof EntityPlayerMP) {
            EntityPlayerMP entityPlayer = (EntityPlayerMP) event.entity;
            deadPlayers.put(entityPlayer.getGameProfile(), entityPlayer);
            HardcoreRevival.spawnPlayerGrave(event.entity.worldObj, (int) event.entity.posX, (int) event.entity.posY, (int) event.entity.posZ, entityPlayer);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        tickTimer++;
        Iterator<EntityPlayerMP> it = deadPlayers.values().iterator();
        while (it.hasNext()) {
            EntityPlayerMP deadPlayer = it.next();
            if (deadPlayer.getHealth() > 0f) {
                it.remove();
                continue;
            }
            if (deadPlayer.playerNetServerHandler != null && !deadPlayer.playerNetServerHandler.netManager.isChannelOpen()) {
                it.remove();
                continue;
            }
            if (HardcoreRevival.enableFireworks && tickTimer % HardcoreRevival.fireworksInterval == 0) {
                EntityFireworkRocket entity = new EntityFireworkRocket(deadPlayer.worldObj, deadPlayer.posX, deadPlayer.posY, deadPlayer.posZ, new ItemStack(Items.fireworks));
                deadPlayer.worldObj.spawnEntityInWorld(entity);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.world.getBlock(event.x, event.y, event.z) != Blocks.skull) {
            return;
        }
        TileEntitySkull skull = (TileEntitySkull) event.world.getTileEntity(event.x, event.y, event.z);
        if (skull.func_152108_a() == null) {
            return;
        }
        ItemStack heldItem = event.entityPlayer.getHeldItem();
        if(event.entityPlayer.isSneaking() && heldItem == null) {
            if(!ForgeHooks.onBlockBreakEvent(event.world, WorldSettings.GameType.SURVIVAL, (EntityPlayerMP) event.entityPlayer, event.x, event.y, event.z).isCanceled()) {
                List<ItemStack> list = Blocks.skull.getDrops(event.world, event.x, event.y, event.z, event.world.getBlockMetadata(event.x, event.y, event.z), 0);
                for (ItemStack itemStack : list) {
                    if (!event.entityPlayer.inventory.addItemStackToInventory(itemStack)) {
                        event.entityPlayer.dropPlayerItemWithRandomChoice(itemStack, false);
                    }
                }
                event.world.setBlockToAir(event.x, event.y, event.z);
            }
            return;
        }
        if (!HardcoreRevival.ritualStructure.checkActivationItem(heldItem)) {
            return;
        }
        if (!HardcoreRevival.ritualStructure.checkStructure(event.world, event.x, event.y, event.z)) {
            event.entityPlayer.addChatMessage(new ChatComponentText("Nothing happens. It appears the ritual structure is not complete."));
            return;
        }
        if (event.entityPlayer.experienceLevel < HardcoreRevival.experienceCost) {
            event.entityPlayer.addChatMessage(new ChatComponentText("Nothing happens. It appears you're just not experienced enough."));
            return;
        }
        if (HardcoreRevival.revivePlayer(skull.func_152108_a().getName(), event.world, event.x, event.y - HardcoreRevival.ritualStructure.getHeadY(), event.z) == null) {
            event.entityPlayer.addChatMessage(new ChatComponentText("Nothing happens. It appears " + skull.func_152108_a().getName() + "'s soul isn't here right now."));
            return;
        }
        if(event.entityPlayer.getHealth() < HardcoreRevival.damageOnRitual) {
            event.entityPlayer.addChatMessage(new ChatComponentText("Well, that was dumb."));
            return;
        }
        HardcoreRevival.ritualStructure.consumeStructure(event.world, event.x, event.y, event.z);
        HardcoreRevival.ritualStructure.consumeActivationItem(heldItem);
        event.world.addWeatherEffect(new EntityLightningBolt(event.world, event.x, event.y - HardcoreRevival.ritualStructure.getHeadY(), event.z));
        if (HardcoreRevival.damageOnRitual > 0) {
            event.entityPlayer.attackEntityFrom(DamageSource.magic, HardcoreRevival.damageOnRitual);
        }
        if (HardcoreRevival.experienceCost > 0) {
            event.entityPlayer.addExperienceLevel(-HardcoreRevival.experienceCost);
        }
        for (ConfiguredPotionEffect potionEffect : HardcoreRevival.effectsOnRitual) {
            event.entityPlayer.addPotionEffect(new PotionEffect(potionEffect.potion.getId(), potionEffect.timeInTicks, potionEffect.potionLevel));
        }
    }

    @SubscribeEvent
    public void onMinecartUpdate(MinecartUpdateEvent event) {
        if(event.minecart.isBurning()) {
            if(event.minecart.getCommandSenderName().equals("GregTheCart")) {
                EntityPlayer greg = HardcoreRevival.revivePlayer("GregTheCart", event.minecart.worldObj, (int) event.x, (int) event.y, (int) event.z);
                if(greg != null) {
                    event.minecart.setDead();
                    event.minecart.worldObj.addWeatherEffect(new EntityLightningBolt(event.minecart.worldObj, event.x, event.y, event.z));
                    greg.setFire(Integer.MAX_VALUE);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        deadPlayers.remove(event.player.getGameProfile());
    }

}
