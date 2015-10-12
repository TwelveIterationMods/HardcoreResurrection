package net.blay09.mods.hardcorerevival;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public class CommandHardcoreRevival extends CommandBase {

    @Override
    public String getCommandName() {
        return "hardcorerevival";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/hardcorerevival (givehead|revive|givebook) [...]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            throw new WrongUsageException(getCommandUsage(sender));
        }
        String cmd = args[0];
        if (cmd.equals("givehead")) {
            EntityPlayer targetPlayer = getPlayer(sender, args.length >= 2 ? args[1] : sender.getCommandSenderName());
            if (targetPlayer == null) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            if (sender instanceof EntityPlayer) {
                EntityPlayer entityPlayer = (EntityPlayer) sender;
                ItemStack itemStack = new ItemStack(Items.skull, 1, 3);
                NBTTagCompound tagCompound = new NBTTagCompound();
                NBTTagCompound ownerCompound = new NBTTagCompound();
                NBTUtil.func_152460_a(ownerCompound, targetPlayer.getGameProfile());
                tagCompound.setTag("SkullOwner", ownerCompound);
                itemStack.setTagCompound(tagCompound);
                if (!entityPlayer.inventory.addItemStackToInventory(itemStack)) {
                    entityPlayer.dropPlayerItemWithRandomChoice(itemStack, false);
                }
            }
        } else if (cmd.equals("givebook")) {
            EntityPlayer targetPlayer = getPlayer(sender, args.length >= 2 ? args[1] : sender.getCommandSenderName());
            if (targetPlayer == null) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            ItemStack helpBookStack = HardcoreRevival.getHelpBook("\u00a7kJohnny\u00a7r");
            if (!targetPlayer.inventory.addItemStackToInventory(helpBookStack)) {
                targetPlayer.dropPlayerItemWithRandomChoice(helpBookStack, false);
            }
        } else if (cmd.equals("revive")) {
            EntityPlayer targetPlayer = getPlayer(sender, args.length >= 2 ? args[1] : sender.getCommandSenderName());
            if (targetPlayer == null) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            HardcoreRevival.revivePlayer(targetPlayer.getCommandSenderName(), sender.getEntityWorld(), sender.getPlayerCoordinates().posX, sender.getPlayerCoordinates().posY, sender.getPlayerCoordinates().posZ);
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "givebook", "givehead", "revive");
        } else if(args.length == 2) {
            return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
        }
        return null;
    }
}
