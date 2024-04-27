package dev.lukebemish.defaultresources.impl.services;

import com.mojang.datafixers.util.Pair;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;

public interface Platform {
	Path getGlobalFolder();

	void extractResources();

	Collection<Pair<String, Pack.ResourcesSupplier>> getJarProviders(PackType type);

	Path getConfigDir();
	Path getResourcePackDir();

	Map<String, Path> getExistingModdedPaths(String relative);

	boolean isClient();
}
