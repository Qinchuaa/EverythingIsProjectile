package com.minecraft26.everythingisprojectile.registry;

import com.minecraft26.everythingisprojectile.EverythingIsProjectileMod;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, EverythingIsProjectileMod.MODID);

    public static final RegistryObject<SoundEvent> GAUNTLET_EQUIP = SOUND_EVENTS.register("gauntlet_equip",
        () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(EverythingIsProjectileMod.MODID, "gauntlet_equip")));

    public static final RegistryObject<SoundEvent> PROJECTILE_LAUNCH = SOUND_EVENTS.register("projectile_launch",
        () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(EverythingIsProjectileMod.MODID, "projectile_launch")));

    private ModSounds() {
    }
}
