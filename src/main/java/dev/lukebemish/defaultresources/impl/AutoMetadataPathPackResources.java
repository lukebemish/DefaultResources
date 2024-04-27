package dev.lukebemish.defaultresources.impl;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.FileUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class AutoMetadataPathPackResources extends AutoMetadataPackResources {
	private static final Logger LOGGER = LogUtils.getLogger();

	protected final List<Path> paths;

	public AutoMetadataPathPackResources(PackLocationInfo info, String prefix, List<Path> paths, PackType packType) {
		super(info, packType, prefix);
		this.paths = paths;
	}

	@Nullable @Override
	public IoSupplier<InputStream> getResource(PackType packType, ResourceLocation location) {
		for (Path root : this.paths) {
			Path path = root.resolve(getPackFolderName()).resolve(location.getNamespace());
			if (!Files.isDirectory(path)) {
				continue;
			}
			return PathPackResources.getResource(location, path);
		}
		return null;
	}

	@Override
	public void listResources(PackType packType, String namespace, String path, ResourceOutput resourceOutput) {
		FileUtil.decomposePath(path).ifSuccess((list) -> {
			for (Path root : this.paths) {
				Path namespacePath = root.resolve(getPackFolderName()).resolve(namespace);
				if (!Files.isDirectory(namespacePath)) {
					continue;
				}
				PathPackResources.listPath(namespace, namespacePath, list, resourceOutput);
			}
		}).ifError((partialResult) -> LOGGER.error("Invalid path {}: {}", path, partialResult.message()));
	}

	@Override
	public Set<String> getNamespaces(PackType type) {
		Set<String> set = new HashSet<>();
		for (Path root : this.paths) {
			Path path = root.resolve(getPackFolderName());

			if (!Files.isDirectory(path)) {
				continue;
			}

			try (DirectoryStream<Path> paths = Files.newDirectoryStream(path)) {
				for (Path namespacePath : paths) {
					if (Files.isDirectory(namespacePath)) {
						String namespace = namespacePath.getFileName().toString();
						if (namespace.equals(namespace.toLowerCase(Locale.ROOT))) {
							set.add(namespace);
						}
					}
				}
			} catch (IOException e) {
				LOGGER.error("Failed to list path {}", path, e);
			}
		}
		return set;
	}

	@Override
	public void close() {

	}
}
