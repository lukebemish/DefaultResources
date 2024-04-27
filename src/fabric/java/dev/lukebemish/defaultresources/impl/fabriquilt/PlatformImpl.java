package dev.lukebemish.defaultresources.impl.fabriquilt;

import com.google.auto.service.AutoService;
import com.mojang.datafixers.util.Pair;
import dev.lukebemish.defaultresources.impl.AutoMetadataPathPackResources;
import dev.lukebemish.defaultresources.impl.DefaultResources;
import dev.lukebemish.defaultresources.impl.Services;
import dev.lukebemish.defaultresources.impl.services.Platform;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import org.jspecify.annotations.NonNull;

@AutoService(Platform.class)
public class PlatformImpl implements Platform {
	@Override
	public Path getGlobalFolder() {
		return FabricLoader.getInstance().getGameDir().resolve("globalresources");
	}

	@Override
	public void extractResources() {
		try {
			if (!Files.exists(Services.PLATFORM.getGlobalFolder()))
				Files.createDirectories(Services.PLATFORM.getGlobalFolder());
		} catch (IOException e) {
			DefaultResources.LOGGER.error(e);
		}
		DefaultResourcesFabriQuilt.forAllModsParallel((modID, path, paths) -> {
			if (!modID.equals("minecraft")) {
				DefaultResources.forMod(path, modID, paths.stream().<Function<String, Path>>map(p -> p::resolve).toList());
			}
		});
	}

	@Override
	public Collection<Pair<String, Pack.ResourcesSupplier>> getJarProviders(PackType type) {
		List<Pair<String, Pack.ResourcesSupplier>> providers = new ArrayList<>();
		DefaultResourcesFabriQuilt.forAllMods((modID, pathGetter, paths) -> {
			if (!modID.equals("minecraft")) {
				providers.add(new Pair<>(modID, new Pack.ResourcesSupplier() {
					@Override
					public PackResources openPrimary(PackLocationInfo info) {
						return new AutoMetadataPathPackResources(info, "global", paths, type);
					}

					@Override
					public PackResources openFull(PackLocationInfo info, Pack.Metadata metadata) {
						return new AutoMetadataPathPackResources(info, "global", paths, type);
					}
				}));
			}
		});
		return providers;
	}

	@Override
	public Path getConfigDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public Path getResourcePackDir() {
		return FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
	}

	@Override
	public Map<String, Path> getExistingModdedPaths(String relative) {
		Map<String, Path> out = new HashMap<>();
		DefaultResourcesFabriQuilt.forAllMods((modID, path, paths) -> {
			if (!modID.equals("minecraft")) {
				Path p = path.apply(relative);
				if (Files.exists(p)) {
					out.put(modID, p);
				}
			}
		});
		return out;
	}

	@Override
	public boolean isClient() {
		return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
	}
}
