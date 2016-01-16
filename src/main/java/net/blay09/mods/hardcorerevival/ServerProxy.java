package net.blay09.mods.hardcorerevival;

import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import net.blay09.mods.hardcorerevival.structure.RitualStructure;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityEnderEye;
import net.minecraft.entity.item.EntityFireworkRocket;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.minecart.MinecartUpdateEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.*;

@SuppressWarnings("unused")
public class ServerProxy extends CommonProxy {

    public static final Map<GameProfile, EntityPlayerMP> deadPlayers = new HashMap<>();
    public static final List<GraveLocator> graveLocators = new ArrayList<>();
    public static final Random random = new Random();

    private int tickTimer;

    public void postInit(FMLPostInitializationEvent event) {
        if (!HardcoreRevival.enableHardcoreRevival) {
            return;
        }
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(this);

        // Prevent skulls from exploding
        Blocks.skull.setResistance(2000f);
        Blocks.skull.blockMaterial = Material.clay;
    }

    @SubscribeEvent
    public void onPlayerJoined(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.player.getHealth() <= 0) {
            deadPlayers.put(event.player.getGameProfile(), (EntityPlayerMP) event.player);
        }
        if(HardcoreRevival.ritualStructureError != null) {
            event.player.addChatMessage(new ChatComponentText("\u00a7c" + HardcoreRevival.ritualStructureError.getMessage()));
        }
    }

    @SubscribeEvent
    public void onPlayerLeft(PlayerEvent.PlayerLoggedOutEvent event) {
        deadPlayers.remove(event.player.getGameProfile());
        HardcoreRevival.unbanHardcoreDeath(event.player.getGameProfile());
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.entity instanceof EntityPlayerMP) {
            EntityPlayerMP entityPlayer = (EntityPlayerMP) event.entity;
            deadPlayers.put(entityPlayer.getGameProfile(), entityPlayer);
            HardcoreRevival.spawnPlayerGrave(event.entity.worldObj, (int) event.entity.posX, (int) event.entity.posY, (int) event.entity.posZ, entityPlayer);
        }
        for(Object obj : MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            EntityPlayer entityPlayer = (EntityPlayer) obj;
            if(entityPlayer != event.entity) {
                NBTTagCompound tagCompound = HardcoreRevival.getHardcoreRevivalData(entityPlayer);
                if (!tagCompound.getBoolean("HelpBook")) {
                    ItemStack itemStack = HardcoreRevival.getHelpBook(event.entity.getCommandSenderName());
                    if(!entityPlayer.inventory.addItemStackToInventory(itemStack)) {
                        entityPlayer.dropPlayerItemWithRandomChoice(itemStack, false);
                    }
                    tagCompound.setBoolean("HelpBook", true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ServerTickEvent event) {
        tickTimer++;
        Iterator<EntityPlayerMP> pit = deadPlayers.values().iterator();
        while (pit.hasNext()) {
            EntityPlayerMP deadPlayer = pit.next();
            if (deadPlayer.getHealth() > 0f) {
                pit.remove();
                continue;
            }
            if (deadPlayer.playerNetServerHandler != null && !deadPlayer.playerNetServerHandler.netManager.isChannelOpen()) {
                pit.remove();
                continue;
            }
            if (HardcoreRevival.enableDeathFireworks && tickTimer % HardcoreRevival.fireworksInterval == 0) {
                EntityFireworkRocket entity = new EntityFireworkRocket(deadPlayer.worldObj, deadPlayer.posX, deadPlayer.posY, deadPlayer.posZ, new ItemStack(Items.fireworks));
                deadPlayer.worldObj.spawnEntityInWorld(entity);
            }
        }
        if(HardcoreRevival.enableCorpseLocating) {
            Iterator<GraveLocator> git = graveLocators.iterator();
            while (git.hasNext()) {
                GraveLocator graveLocator = git.next();
                if (graveLocator.entityItem.isDead) {
                    git.remove();
                    continue;
                }
                if (graveLocator.entityItem.isBurning()) {
                    ItemStack itemStack = graveLocator.entityItem.getEntityItem();
                    EntityPlayer entityPlayer = MinecraftServer.getServer().getConfigurationManager().func_152612_a(itemStack.getDisplayName());
                    if (entityPlayer != null) {
                        if (entityPlayer.getHealth() <= 0) {
                            EntityEnderEye entityEnderEye = new EntityEnderEye(graveLocator.entityItem.worldObj, graveLocator.entityItem.posX, graveLocator.entityItem.posY, graveLocator.entityItem.posZ);
                            if (entityPlayer.dimension != entityEnderEye.dimension) {
                                entityEnderEye.moveTowards(entityPlayer.posX, graveLocator.entityItem.worldObj.getHeight(), entityPlayer.posZ);
                            } else {
                                entityEnderEye.moveTowards(entityPlayer.posX, (int) entityPlayer.posY, entityPlayer.posZ);
                            }
                            entityEnderEye.shatterOrDrop = false;
                            graveLocator.entityItem.worldObj.spawnEntityInWorld(entityEnderEye);
                        } else {
                            graveLocator.owner.addChatMessage(new ChatComponentText("You can't locate souls that reside in the world of the living, silly."));
                        }
                    } else {
                        graveLocator.owner.addChatMessage(new ChatComponentText("Nothing happens. It appears " + itemStack.getDisplayName() + "'s soul isn't here right now."));
                    }
                    git.remove();
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerInteractWithEntity(EntityInteractEvent event) {
        if(HardcoreRevival.enableSillyThings) {
            if(event.entityPlayer instanceof FakePlayer && HardcoreRevival.disallowFakePlayers) {
                return;
            }
            if(event.target instanceof EntityOcelot && ((EntityOcelot) event.target).isTamed() && ((EntityOcelot) event.target).getCustomNameTag().equals("Gamerkitty_1")) {
                if (event.entityPlayer.getHeldItem() != null && event.entityPlayer.getHeldItem().getItem() == Items.golden_apple) {
                    EntityPlayerMP deadKitty = MinecraftServer.getServer().getConfigurationManager().func_152612_a("Gamerkitty_1");
                    EntityPlayer kitty = HardcoreRevival.revivePlayer(deadKitty, event.target.worldObj, (int) event.target.posX, (int) event.target.posY, (int) event.target.posZ);
                    if (kitty != null) {
                        event.target.setDead();
                        kitty.worldObj.addWeatherEffect(new EntityLightningBolt(kitty.worldObj, kitty.posX, kitty.posY, kitty.posZ));
                        for(int i = 0; i < 5; i++) {
                            double motionX = random.nextGaussian() * 0.02;
                            double motionY = random.nextGaussian() * 0.02;
                            double motionZ = random.nextGaussian() * 0.02;
                            event.target.worldObj.spawnParticle("heart", kitty.posX + (random.nextFloat() * kitty.width * 3f) - kitty.width, kitty.posY + 0.5 + (random.nextFloat() * kitty.height), kitty.posZ + (random.nextFloat() * kitty.width * 3f) - kitty.width, motionX, motionY, motionZ);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(event.entityPlayer instanceof FakePlayer && HardcoreRevival.disallowFakePlayers) {
            return;
        }
        if (event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack heldItem = event.entityPlayer.getHeldItem();
        boolean isDebugging = heldItem != null && heldItem.getItem() == Items.stick && heldItem.hasTagCompound() && heldItem.getTagCompound().hasKey("HardcoreRevivalDebugger");
        boolean isBuilding = heldItem != null && heldItem.getItem() == Items.stick && heldItem.hasTagCompound() && heldItem.getTagCompound().hasKey("HardcoreRevivalBuilder");
        if(isBuilding) {
            if(heldItem.getTagCompound().hasKey("HardcoreRevivalBuilderCoords")) {
                int[] first = heldItem.getTagCompound().getIntArray("HardcoreRevivalBuilderCoords");
                RitualStructure.exportRitual(event.world, first[0], first[1], first[2], event.x, event.y, event.z);
                event.entityPlayer.addChatMessage(new ChatComponentText("Structure has been exported to generated.json"));
            } else {
                heldItem.getTagCompound().setIntArray("HardcoreRevivalBuilderCoords", new int[] {event.x, event.y, event.z});
                event.entityPlayer.addChatMessage(new ChatComponentText("Coordinates stored, now rightclick the opposite outer corner."));
            }
            return;
        }
        if(HardcoreRevival.ritualStructure == null) {
            if(isDebugging) {
                event.entityPlayer.addChatMessage(new ChatComponentText("The currently loaded ritual structure is invalid."));
            }
            return;
        }
        Block block = event.world.getBlock(event.x, event.y, event.z);
        if (block != Blocks.skull) {
            if(isDebugging) {
                GameRegistry.UniqueIdentifier identifier = GameRegistry.findUniqueIdentifierFor(block);
                StringBuilder sb = new StringBuilder();
                if(identifier != null) {
                    sb.append(identifier.modId).append(":").append(identifier.name);
                } else {
                    sb.append(block.getUnlocalizedName());
                }
                sb.append(":").append(event.world.getBlockMetadata(event.x, event.y, event.z));
                sb.append(" at ").append(event.x).append(", ").append(event.y).append(", ").append(event.z);
                event.entityPlayer.addChatMessage(new ChatComponentText(sb.toString()));
            }
            return;
        }
        TileEntitySkull skull = (TileEntitySkull) event.world.getTileEntity(event.x, event.y, event.z);
        if (!isDebugging && skull.func_152108_a() == null) {
            event.entityPlayer.addChatMessage(new ChatComponentText("This head is not one of a dead soul."));
            return;
        }
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
        if (!isDebugging && !HardcoreRevival.ritualStructure.checkActivationItem(heldItem)) {
            return;
        }
        if (!HardcoreRevival.ritualStructure.checkStructure(event.entityPlayer, event.world, event.x, event.y, event.z, isDebugging)) {
            if(!isDebugging) {
                event.entityPlayer.addChatMessage(new ChatComponentText("Nothing happens. It appears the ritual structure is not complete."));
            }
            return;
        }
        if(isDebugging) {
            event.entityPlayer.addChatMessage(new ChatComponentText("Success! Ritual structure is valid."));
            return;
        }
        if (event.entityPlayer.experienceLevel < HardcoreRevival.experienceCost) {
            event.entityPlayer.addChatMessage(new ChatComponentText("Nothing happens. It appears you're just not experienced enough."));
            return;
        }
        EntityPlayerMP deadPlayer = MinecraftServer.getServer().getConfigurationManager().func_152612_a(skull.func_152108_a().getName());
        if(deadPlayer == null) {
            event.entityPlayer.addChatMessage(new ChatComponentText("Nothing happens. It appears the soul is not reachable right now."));
            return;
        }
        if(deadPlayer.getHealth() > 0) {
            event.entityPlayer.addChatMessage(new ChatComponentText("You can't revive souls that reside in the world of the living, silly."));
            return;
        }
        HardcoreRevival.ritualStructure.consumeStructure(event.world, event.x, event.y, event.z);
        HardcoreRevival.ritualStructure.consumeActivationItem(heldItem);
        if (HardcoreRevival.revivePlayer(deadPlayer, event.world, event.x, event.y - HardcoreRevival.ritualStructure.getHeadY(), event.z) == null) {
            event.entityPlayer.addChatMessage(new ChatComponentText("Nothing happens. It appears " + skull.func_152108_a().getName() + " has angered the gods."));
            return;
        }
        if(event.entityPlayer.getHealth() < HardcoreRevival.damageOnRitual) {
            event.entityPlayer.addChatMessage(new ChatComponentText("Well, that was dumb."));
        }
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
                EntityPlayerMP deadGreg = MinecraftServer.getServer().getConfigurationManager().func_152612_a("GregTheCart");
                EntityPlayer greg = HardcoreRevival.revivePlayer(deadGreg, event.minecart.worldObj, (int) event.x, (int) event.y, (int) event.z);
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

    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        if(HardcoreRevival.enableCorpseLocating) {
            ItemStack itemStack = event.entityItem.getEntityItem();
            if (itemStack.isItemEqual(HardcoreRevival.locatorItem) && itemStack.hasDisplayName()) {
                graveLocators.add(new GraveLocator(event.player, event.entityItem));
            }
        }
    }
}
