package com.minecraft26.everythingisprojectile.registry;

import net.minecraftforge.event.AddPackFindersEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StableEnchantmentPack extends StableRegistryPack {
    private static final StableEnchantmentPack INSTANCE = new StableEnchantmentPack();
    private static final String MIGHT_JSON = """
            {
              "description": {
                "translate": "enchantment.everythingisprojectile.might"
              },
              "supported_items": [
                "everythingisprojectile:projectile_gauntlet",
                "everythingisprojectile:iron_projectile_gauntlet",
                "everythingisprojectile:diamond_projectile_gauntlet",
                "everythingisprojectile:creative_projectile_gauntlet"
              ],
              "primary_items": [
                "everythingisprojectile:projectile_gauntlet",
                "everythingisprojectile:iron_projectile_gauntlet",
                "everythingisprojectile:diamond_projectile_gauntlet",
                "everythingisprojectile:creative_projectile_gauntlet"
              ],
              "weight": 4,
              "max_level": 5,
              "min_cost": {
                "base": 8,
                "per_level_above_first": 8
              },
              "max_cost": {
                "base": 18,
                "per_level_above_first": 10
              },
              "anvil_cost": 4,
              "slots": [
                "hand"
              ],
              "effects": {}
            }
            """;

    // 向服务端数据包仓库注入此模组附魔的稳定数据包
    public static void onAddPackFinders(AddPackFindersEvent event) {
        INSTANCE.handleAddPackFinders(event);
    }

    @Override
    protected String packIdSuffix() {
        return "enchantments";
    }

    @Override
    protected void writeEntries(Path packRoot) throws IOException {
        Path enchantmentDir = packRoot.resolve("data").resolve("everythingisprojectile").resolve("enchantment");
        Files.createDirectories(enchantmentDir);
        Files.writeString(enchantmentDir.resolve("might.json"), MIGHT_JSON, StandardCharsets.UTF_8);
    }

    private StableEnchantmentPack() {
    }
}
