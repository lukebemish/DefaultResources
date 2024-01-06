/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl.neoforge;

import com.google.auto.service.AutoService;
import com.mojang.datafixers.util.Pair;
import dev.lukebemish.defaultresources.impl.AutoMetadataPathPackResources;
import dev.lukebemish.defaultresources.impl.DefaultResources;
import dev.lukebemish.defaultresources.impl.Services;
import dev.lukebemish.defaultresources.impl.services.Platform;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AutoService(Platform.class)
public class PlatformImpl implements Platform {

    public Path getGlobalFolder() {
        return FMLPaths.GAMEDIR.get().resolve("globalresources");
    }

    @Override
    public void extractResources() {
        try {
            if (!Files.exists(Services.PLATFORM.getGlobalFolder()))
                Files.createDirectories(Services.PLATFORM.getGlobalFolder());
        } catch (IOException e) {
            DefaultResources.LOGGER.error(e);
        }
        FMLLoader.getLoadingModList().getModFiles().stream().flatMap(f -> f.getMods().stream())
            .filter(PlatformImpl::isExtractable)
            .forEach(mod ->
                DefaultResources.forMod(mod.getOwningFile().getFile()::findResource, mod.getModId()));
    }

    @Override
    public Collection<Pair<String, Pack.ResourcesSupplier>> getJarProviders(PackType type) {
        List<Pair<String, Pack.ResourcesSupplier>> providers = new ArrayList<>();
        FMLLoader.getLoadingModList().getModFiles().stream().flatMap(f -> f.getMods().stream())
            .filter(PlatformImpl::isExtractable)
            .forEach(mod -> {
                Path packPath = mod.getOwningFile().getFile().getSecureJar().getPath(String.join("/"));
                providers.add(new Pair<>(mod.getModId(), new Pack.ResourcesSupplier() {
                    @Override
                    public @NonNull PackResources openPrimary(@NonNull String s) {
                        return new AutoMetadataPathPackResources(s, "global", packPath, type);
                    }

                    @Override
                    public @NonNull PackResources openFull(@NonNull String s, Pack.@NonNull Info info) {
                        return new AutoMetadataPathPackResources(s, "global", packPath, type);
                    }
                }));
            });
        return providers;
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Path getResourcePackDir() {
        return FMLPaths.GAMEDIR.get().resolve("resourcepacks");
    }

    @Override
    public Map<String, Path> getExistingModdedPaths(String relative) {
        return FMLLoader.getLoadingModList().getModFiles().stream().flatMap(f -> f.getMods().stream())
            .filter(PlatformImpl::isExtractable)
            .map(mod ->
                new Pair<>(mod.getModId(), mod.getOwningFile().getFile().findResource(relative)))
            .filter(it -> it.getSecond() != null && Files.exists(it.getSecond()))
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (a, b) -> a));
    }

    private static boolean isExtractable(IModInfo mod) {
        return !mod.getModId().equals("forge") && !mod.getModId().equals("neoforge") && !mod.getModId().equals("minecraft");
    }

    @Override
    public boolean isClient() {
        return FMLLoader.getDist() == Dist.CLIENT;
    }
}
