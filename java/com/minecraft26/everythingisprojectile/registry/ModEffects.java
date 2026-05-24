package com.minecraft26.everythingisprojectile.registry;

import com.minecraft26.everythingisprojectile.EverythingIsProjectileMod;
import com.minecraft26.everythingisprojectile.effect.BleedingMobEffect;
import com.minecraft26.everythingisprojectile.effect.ColdMobEffect;
import com.minecraft26.everythingisprojectile.effect.HeadhurtMobEffect;
import com.minecraft26.everythingisprojectile.effect.NauseaMobEffect;
import com.minecraft26.everythingisprojectile.effect.OnFireMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
        DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, EverythingIsProjectileMod.MODID);

    public static final RegistryObject<MobEffect> HEADHURT = MOB_EFFECTS.register("headhurt", HeadhurtMobEffect::new);
    public static final RegistryObject<MobEffect> NAUSEA = MOB_EFFECTS.register("nausea", NauseaMobEffect::new);
    public static final RegistryObject<MobEffect> BLEEDING = MOB_EFFECTS.register("bleeding", BleedingMobEffect::new);
    public static final RegistryObject<MobEffect> ON_FIRE = MOB_EFFECTS.register("onfire", OnFireMobEffect::new);
    public static final RegistryObject<MobEffect> FREEZING = MOB_EFFECTS.register("freezing", ColdMobEffect::new);

    private ModEffects() {
    }
}
