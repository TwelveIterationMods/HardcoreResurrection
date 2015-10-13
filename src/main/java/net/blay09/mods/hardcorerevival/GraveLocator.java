package net.blay09.mods.hardcorerevival;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;

public class GraveLocator {
    public final EntityPlayer owner;
    public final EntityItem entityItem;

    public GraveLocator(EntityPlayer owner, EntityItem entityItem) {
        this.owner = owner;
        this.entityItem = entityItem;
    }
}
