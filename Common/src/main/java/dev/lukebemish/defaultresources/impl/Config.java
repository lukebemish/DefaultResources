/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl;

import com.google.common.base.Suppliers;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.ZipFile;

public record Config(HashMap<String, ExtractionState> extract, HashMap<String, Boolean> fromResourcePacks) {
    public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.unboundedMap(Codec.STRING, StringRepresentable.fromEnum(ExtractionState::values)).xmap(HashMap::new, Function.identity()).fieldOf("extract").forGetter(Config::extract),
        Codec.unboundedMap(Codec.STRING, Codec.BOOL).fieldOf("from_resource_packs").xmap(HashMap::new, Function.identity()).forGetter(Config::fromResourcePacks)
    ).apply(i, Config::new));

    public static final Supplier<Config> INSTANCE = Suppliers.memoize(Config::readFromConfig);

    private static Config getDefault() {
        return new Config(new HashMap<>(), new HashMap<>());
    }

    private static Config readFromConfig() {
        Path configPath = Services.PLATFORM.getConfigDir().resolve(DefaultResources.MOD_ID + ".json");
        Config config = getDefault();
        if (Files.exists(configPath)) {
            try {
                JsonElement json = DefaultResources.GSON.fromJson(Files.newBufferedReader(configPath), JsonElement.class);
                config = CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, e ->
                    DefaultResources.LOGGER.error("Error parsing {}.json config; using (and replacing) with default: {}", DefaultResources.MOD_ID, e));
            } catch (IOException e) {
                DefaultResources.LOGGER.error("Could not read {}.json config; using (and replacing) with default.", DefaultResources.MOD_ID, e);
            } catch (JsonSyntaxException e) {
                DefaultResources.LOGGER.error("Error parsing {}.json config; using (and replacing) with default.", DefaultResources.MOD_ID, e);
            } catch (RuntimeException ignored) {
                // Already caught and logged.
            }
        }
        var map = new HashMap<>(config.extract());
        Services.PLATFORM.getExistingModdedPaths(DefaultResources.META_FILE_PATH).forEach((modId, metaPath) -> {
            try (var is = Files.newInputStream(metaPath)) {
                JsonElement object = DefaultResources.GSON.fromJson(new InputStreamReader(is), JsonElement.class);
                ModMetaFile metaFile = ModMetaFile.CODEC.parse(JsonOps.INSTANCE, object).getOrThrow(false, e -> {
                });
                if (!map.containsKey(modId)) {
                    map.put(modId, metaFile.extractsByDefault() ? ExtractionState.EXTRACT : ExtractionState.UNEXTRACTED);
                }
            } catch (IOException | RuntimeException e) {
                DefaultResources.LOGGER.warn("We thought there was a readable {} for mod {}, but we got an error when reading it!",
                    DefaultResources.META_FILE_PATH, modId, e);
            }
        });

        var resourcePacks = new HashMap<String, Boolean>();
        var originalResourcePacks = config.fromResourcePacks();
        Path resourcePacksPath = Services.PLATFORM.getResourcePackDir();
        try (var paths = Files.list(resourcePacksPath)) {
            paths.forEach(path -> {
                String fileName = path.getFileName().toString();
                boolean detect;
                if (fileName.endsWith(".zip") || fileName.endsWith(".jar")) {
                    detect = checkZipForMeta(path);
                } else if (Files.isDirectory(path)) {
                    detect = checkPathForMeta(path);
                } else {
                    return;
                }
                if (detect) {
                    resourcePacks.put(fileName, originalResourcePacks.getOrDefault(fileName, true));
                }
            });
        } catch (IOException e) {
            if (Files.exists(resourcePacksPath)) {
                DefaultResources.LOGGER.warn("Could not read resource packs from {}!", resourcePacksPath, e);
            }
        }

        config = new Config(map, resourcePacks);

        try {
            writeConfig(configPath, config);
        } catch (IOException e) {
            DefaultResources.LOGGER.error("Could not write {}.json config. There may be odd behavior. ", DefaultResources.MOD_ID, e);
        }

        return config;
    }

    private static boolean checkZipForMeta(Path path) {
        try (var zipFile = new ZipFile(path.toFile());
             var reader = zipFile.getInputStream(zipFile.getEntry("pack.mcmeta"))) {
            if (reader != null) {
                JsonObject object = DefaultResources.GSON.fromJson(new InputStreamReader(reader), JsonObject.class);
                JsonElement meta = object.get(DefaultResources.MOD_ID);
                var result = DefaultResourcesMetadataSection.CODEC.parse(JsonOps.INSTANCE, meta);
                return result.result().isPresent() && result.result().get().detect();
            }
        } catch (Exception e) {
            DefaultResources.LOGGER.error("Could not read {}, which looks like a zip file, for resource pack detection; ignoring.", path.getFileName(), e);
        }
        return false;
    }

    private static boolean checkPathForMeta(Path path) {
        var metaFile = path.resolve("pack.mcmeta");
        if (Files.exists(metaFile)) {
            try (var reader = Files.newBufferedReader(metaFile)) {
                JsonObject object = DefaultResources.GSON.fromJson(reader, JsonObject.class);
                JsonElement meta = object.get(DefaultResources.MOD_ID);
                var result = DefaultResourcesMetadataSection.CODEC.parse(JsonOps.INSTANCE, meta);
                return result.result().isPresent() && result.result().get().detect();
            } catch (Exception e) {
                DefaultResources.LOGGER.error("Could not read {} for resource pack detection; ignoring.", path.getFileName(), e);
            }
        }
        return false;
    }

    private static void writeConfig(Path path, Config config) throws IOException {
        if (!Files.exists(path.getParent()))
            Files.createDirectories(path.getParent());
        Files.deleteIfExists(path);
        try (var writer = Files.newBufferedWriter(path)) {
            writer.write("// Set extraction to \"extract\" to extract currently unextracted resources.\n");
            DefaultResources.GSON.toJson(CODEC.encodeStart(JsonOps.INSTANCE, config)
                    .getOrThrow(false, e -> {
                    }),
                writer);
        }
    }

    public void save() {
        Path configPath = Services.PLATFORM.getConfigDir().resolve(DefaultResources.MOD_ID + ".json");
        try {
            writeConfig(configPath, this);
        } catch (IOException e) {
            DefaultResources.LOGGER.error("Could not save {} config!", DefaultResources.MOD_ID, e);
        }
    }

    enum ExtractionState implements StringRepresentable {
        UNEXTRACTED(false, false),
        EXTRACT(true, true),
        EXTRACTED(true, false);

        public final boolean extractIfMissing;
        public final boolean extractRegardless;

        ExtractionState(boolean extractIfMissing, boolean extractRegardless) {
            this.extractIfMissing = extractIfMissing;
            this.extractRegardless = extractRegardless;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
