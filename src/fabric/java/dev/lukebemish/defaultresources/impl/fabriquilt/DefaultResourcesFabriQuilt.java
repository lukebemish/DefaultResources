package dev.lukebemish.defaultresources.impl.fabriquilt;

import com.mojang.datafixers.util.Pair;
import dev.lukebemish.defaultresources.impl.DefaultResources;
import dev.lukebemish.defaultresources.impl.ParallelExecutor;
import dev.lukebemish.defaultresources.impl.Services;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.world.flag.FeatureFlagSet;

@SuppressWarnings("deprecation")
public class DefaultResourcesFabriQuilt implements ModInitializer {
	@Override
	public void onInitialize() {
		DefaultResources.initialize();
		addPackResources(PackType.SERVER_DATA);
	}

	public interface PerModAction {
		void accept(String modID, Function<String, Path> path, List<Path> rootPaths);
	}

	public static void forAllMods(PerModAction consumer) {
		FabricLoader.getInstance().getAllMods().forEach(mod -> consumer.accept(mod.getMetadata().getId(), s -> mod.findPath(s).orElseGet(() -> mod.getRootPath().resolve(s)), mod.getRootPaths()));
	}

	public static void forAllModsParallel(PerModAction consumer) {
		ParallelExecutor.execute(FabricLoader.getInstance().getAllMods().stream(), mod -> consumer.accept(mod.getMetadata().getId(), s -> mod.findPath(s).orElseGet(() -> mod.getRootPath().resolve(s)), mod.getRootPaths()));
	}

	public static void addPackResources(PackType type) {
		try {
			if (!Files.exists(Services.PLATFORM.getGlobalFolder()))
				Files.createDirectories(Services.PLATFORM.getGlobalFolder());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		ResourceLoader.get(type).addPacks(() -> {
			List<PackResources> out = new ArrayList<>();
			List<Pair<String, Pack.ResourcesSupplier>> packs = DefaultResources.getPackResources(type);
			for (var pair : packs) {
				Pack.Metadata info = new Pack.Metadata(
					Component.literal("Global Resources - "+pair.getFirst()),
					PackCompatibility.COMPATIBLE,
					FeatureFlagSet.of(),
					List.of()
				);
				out.add(pair.getSecond().openFull(DefaultResources.infoFor(pair.getFirst()), info));
			}
			return out;
		});
	}
}
