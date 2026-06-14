package com.minecraft26.everythingisprojectile.registry;

import com.minecraft26.everythingisprojectile.EverythingIsProjectileMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public abstract class StableRegistryPack {
    private static final PackSelectionConfig SELECTION_CONFIG = new PackSelectionConfig(true, Pack.Position.TOP, false);

    // 返回数据包 ID 后缀
    protected abstract String packIdSuffix();

    // 将注册条目文件写入数据包根目录
    protected abstract void writeEntries(Path packRoot) throws IOException;

    // 可覆写：数据包描述文本
    protected String packDescription() {
        return "EverythingIsProjectile stable registry pack";
    }

    // 可覆写：KnownPack 版本号
    protected String knownPackVersion() {
        return "1";
    }

    // 向服务端数据包仓库注入此稳定数据包
    protected final void handleAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) return;
        event.addRepositorySource(consumer -> {
            Pack pack = createPack();
            if (pack != null) consumer.accept(pack);
        });
    }

    // 创建携带 KnownPack 的数据包
    private Pack createPack() {
        try {
            Path packRoot = writePackFiles();
            PackLocationInfo location = new PackLocationInfo(
                    fullPackId(),
                    Component.literal(packDescription()),
                    PackSource.BUILT_IN,
                    Optional.of(new KnownPack("everythingisprojectile", packIdSuffix(), knownPackVersion()))
            );
            return Pack.readMetaAndCreate(
                    location,
                    new PathPackResources.PathResourcesSupplier(packRoot),
                    PackType.SERVER_DATA,
                    SELECTION_CONFIG
            );
        } catch (IOException exception) {
            EverythingIsProjectileMod.LOGGER.error("Failed to create stable registry pack", exception);
            return null;
        }
    }

    // 将数据包 meta 与条目写入运行目录
    private Path writePackFiles() throws IOException {
        Path packRoot = FMLPaths.GAMEDIR.get().resolve(fullPackId());
        writeFile(packRoot.resolve("pack.mcmeta"), packMeta());
        writeEntries(packRoot);
        return packRoot;
    }

    private String packMeta() {
        return "{\"pack\":{\"description\":\"" + packDescription()
                + "\",\"max_format\":101,\"min_format\":[101,1]}}";
    }

    private static void writeFile(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private String fullPackId() {
        return "everythingisprojectile_" + packIdSuffix();
    }
}
