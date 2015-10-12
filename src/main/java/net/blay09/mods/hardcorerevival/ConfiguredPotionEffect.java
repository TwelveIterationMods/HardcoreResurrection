package net.blay09.mods.hardcorerevival;

import net.minecraft.potion.Potion;

public class ConfiguredPotionEffect {

    public final int timeInTicks;
    public final Potion potion;
    public final int potionLevel;

    public ConfiguredPotionEffect(String s) {
        int timeIdx = s.indexOf('x');
        if(timeIdx != -1) {
            timeInTicks = Integer.parseInt(s.substring(0, timeIdx));
        } else {
            timeInTicks = 20 * 12;
        }
        int levelIdx = s.lastIndexOf('@');
        if(levelIdx != -1) {
            potionLevel = Integer.parseInt(s.substring(levelIdx + 1));
        } else {
            potionLevel = 1;
        }
        String potionName = s.substring(timeIdx != -1 ? timeIdx + 1 : 0, levelIdx != -1 ? levelIdx : s.length());
        Potion foundPotion = null;
        for(Potion potion : Potion.potionTypes) {
            if(potion == null) {
                continue;
            }
            if(potion.getName().equals(potionName)) {
                foundPotion = potion;
                break;
            }
        }
        if(foundPotion == null) {
            try {
                int potionId = Integer.parseInt(potionName);
                for (Potion potion : Potion.potionTypes) {
                    if(potion == null) {
                        continue;
                    }
                    if (potion.getId() == potionId) {
                        foundPotion = potion;
                        break;
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
        if(foundPotion == null) {
            throw new RuntimeException("Configured Hardcore Revival respawn potion effect " + potionName + " could not be found");
        }
        this.potion = foundPotion;
    }
}
