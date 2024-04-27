package dev.lukebemish.defaultresources.impl;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipFile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;

public class AutoMetadataFilePackResources extends AutoMetadataPackResources {
	private final ZipFileHolder zipFileHolder;
	private final Path path;
	public AutoMetadataFilePackResources(PackLocationInfo info, String prefix, Path path, PackType packType) {
		super(info, packType, prefix);
		this.path = path;
		this.zipFileHolder = new ZipFileHolder();
	}

	private String getPathFromLocation(PackType packType, ResourceLocation location) {
		return String.format(Locale.ROOT, "%s/%s/%s", this.getPackFolderName(), location.getNamespace(), location.getPath());
	}

	public IoSupplier<InputStream> getResource(PackType packType, ResourceLocation location) {
		return this.getResource(getPathFromLocation(packType, location));
	}

	@Nullable private IoSupplier<InputStream> getResource(String resourcePath) {
		var zipFile = this.zipFileHolder.getOrCreateZipFile();
		if (zipFile == null) {
			return null;
		} else {
			var entry = zipFile.getEntry(resourcePath);
			return entry == null ? null : IoSupplier.create(zipFile, entry);
		}
	}

	public Set<String> getNamespaces(PackType type) {
		var zipFile = this.zipFileHolder.getOrCreateZipFile();
		if (zipFile == null) {
			return Set.of();
		} else {
			Set<String> set = Sets.newHashSet();
			var prefix = getPackFolderName() + "/";

			for (var entry : ofEnumeration(zipFile.entries())) {
				var entryPath = entry.getName();
				var namespace = "";
				if (entryPath.startsWith(prefix)) {
					var parts = entryPath.substring(prefix.length()).split("/");
					if (parts.length != 0) {
						namespace = parts[0];
					}
				}
				if (!namespace.isEmpty()) {
					if (ResourceLocation.isValidNamespace(namespace)) {
						set.add(namespace);
					} else {
						DefaultResources.LOGGER.warn(AutoMetadataFilePackResources.class.getSimpleName()+": Non [a-z0-9_.-] character in namespace {} in pack {}, ignoring", namespace, this.path);
					}
				}
			}

			return set;
		}
	}

	private static <T> Iterable<T> ofEnumeration(Enumeration<T> enumeration) {
		return enumeration::asIterator;
	}

	public void listResources(PackType packType, String namespace, String path, PackResources.ResourceOutput resourceOutput) {
		var zipFile = this.zipFileHolder.getOrCreateZipFile();
		if (zipFile != null) {
			var namespacePrefix = getPackFolderName() + "/" + namespace + "/";
			var pathPrefix = namespacePrefix + path + "/";
			for (var entry : ofEnumeration(zipFile.entries())) {
				if (!entry.isDirectory()) {
					var entryPath = entry.getName();
					if (entryPath.startsWith(pathPrefix)) {
						var location = ResourceLocation.tryBuild(namespace, entryPath.substring(namespacePrefix.length()));
						if (location != null) {
							resourceOutput.accept(location, IoSupplier.create(zipFile, entry));
						} else {
							DefaultResources.LOGGER.warn(AutoMetadataFilePackResources.class.getSimpleName()+": Invalid path in datapack: {}:{}, ignoring", namespace, entryPath);
						}
					}
				}
			}
		}
	}

	private class ZipFileHolder implements AutoCloseable {
		@Nullable private ZipFile zipFile;
		private boolean loaded;

		@Nullable ZipFile getOrCreateZipFile() {
			if (zipFile == null && this.loaded) {
				return null;
			} else {
				if (this.zipFile == null) {
					this.loaded = true;
					try {
						this.zipFile = new ZipFile(AutoMetadataFilePackResources.this.path.toFile());
					} catch (IOException var2) {
						DefaultResources.LOGGER.error(AutoMetadataFilePackResources.class.getSimpleName()+": Failed to open pack {}", AutoMetadataFilePackResources.this.path, var2);
						return null;
					}
				}

				return this.zipFile;
			}
		}

		public void close() {
			if (this.zipFile != null) {
				IOUtils.closeQuietly(this.zipFile);
				this.zipFile = null;
			}
		}
	}

	@Override
	public void close() {
		this.zipFileHolder.close();
	}
}
