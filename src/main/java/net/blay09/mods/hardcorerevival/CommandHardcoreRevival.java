package net.blay09.mods.hardcorerevival;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public class CommandHardcoreRevival extends CommandBase {

    @Override
    public String getCommandName() {
        return "hardcorerevival";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/hardcorerevival (givehead|givebook|spawngrave|revive) [...]";
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
                ItemStack itemStack = HardcoreRevival.getPlayerHead(targetPlayer);
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
            EntityPlayerMP targetPlayer = getPlayer(sender, args.length >= 2 ? args[1] : sender.getCommandSenderName());
            if (targetPlayer == null) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            HardcoreRevival.revivePlayer(targetPlayer, sender.getEntityWorld(), sender.getPlayerCoordinates().posX, sender.getPlayerCoordinates().posY, sender.getPlayerCoordinates().posZ);
        } else if (cmd.equals("spawngrave")) {
            EntityPlayer targetPlayer = getPlayer(sender, args.length >= 2 ? args[1] : sender.getCommandSenderName());
            if (targetPlayer == null) {
                throw new WrongUsageException(getCommandUsage(sender));
            }
            HardcoreRevival.spawnPlayerGrave(sender.getEntityWorld(), sender.getPlayerCoordinates().posX, sender.getPlayerCoordinates().posY, sender.getPlayerCoordinates().posZ, targetPlayer);
        } else {
            throw new WrongUsageException(getCommandUsage(sender));
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "givebook", "givehead", "revive", "spawngrave");
        } else if(args.length == 2) {
            return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
        }
        return null;
    }
}
