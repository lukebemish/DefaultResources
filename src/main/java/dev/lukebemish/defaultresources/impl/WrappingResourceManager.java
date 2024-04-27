package dev.lukebemish.defaultresources.impl;

import dev.lukebemish.defaultresources.api.GlobalResourceManager;
import dev.lukebemish.defaultresources.impl.mixin.ResourceAccessor;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class WrappingResourceManager implements GlobalResourceManager {

	private final GlobalResourceManager wrapped;
	private final String prefix;

	private static void checkValidPrefix(String prefix) {
		if (prefix.isEmpty()) {
			throw new IllegalArgumentException("Invalid prefix; prefixes must not be empty.");
		}
		for (char c : prefix.toCharArray()) {
			if (c == '_' || c == '-' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
				continue;
			}
			throw new IllegalArgumentException("Invalid prefix: \"" + prefix+"\". Prefixes must contain only lowercase letters, numbers, underscores, and dashes.");
		}
	}

	public WrappingResourceManager(GlobalResourceManager wrapped, String prefix) {
		checkValidPrefix(prefix);
		this.wrapped = wrapped;
		this.prefix = prefix+"/";
	}

	@Override
	public Set<String> getNamespaces() {
		return wrapped.getNamespaces();
	}

	@Override
	public List<Resource> getResourceStack(ResourceLocation location) {
		List<Resource> out = new ArrayList<>();
		for (var resource : wrapped.getResourceStack(location.withPrefix(prefix))) {
			out.add(wrapResource(resource));
		}
		return out;
	}

	@Override
	public Map<ResourceLocation, Resource> listResources(String path, Predicate<ResourceLocation> filter) {
		Map<ResourceLocation, Resource> out = new HashMap<>();
		for (var entry : wrapped.listResources(prefix + path, filter).entrySet()) {
			out.put(wrapLocation(entry.getKey()), wrapResource(entry.getValue()));
		}
		return out;
	}

	@Override
	public Map<ResourceLocation, List<Resource>> listResourceStacks(String path, Predicate<ResourceLocation> filter) {
		Map<ResourceLocation, List<Resource>> out = new HashMap<>();
		for (var entry : wrapped.listResourceStacks(prefix + path, filter).entrySet()) {
			List<Resource> resources = new ArrayList<>();
			for (var resource : entry.getValue()) {
				resources.add(wrapResource(resource));
			}
			out.put(wrapLocation(entry.getKey()), resources);
		}
		return out;
	}

	@Override
	public Stream<PackResources> listPacks() {
		return wrapped.listPacks().map(this::wrapResources);
	}

	private Resource wrapResource(Resource resource) {
		if (((ResourceAccessor) resource).defaultresources_getMetadataSupplier() == ResourceMetadata.EMPTY_SUPPLIER) {
			return new Resource(wrapResources(resource.source()), ((ResourceAccessor) resource).defaultresources_getStreamSupplier());
		}
		return new Resource(wrapResources(resource.source()), ((ResourceAccessor) resource).defaultresources_getStreamSupplier(), ((ResourceAccessor) resource).defaultresources_getMetadataSupplier());
	}

	private ResourceLocation wrapLocation(ResourceLocation location) {
		return location.withPath(s -> s.substring(prefix.length()));
	}

	private PackResources wrapResources(PackResources pack) {
		return new PackResources() {
			@Nullable @Override
			public IoSupplier<InputStream> getRootResource(String... elements) {
				return pack.getRootResource(elements);
			}

			@Nullable @Override
			public IoSupplier<InputStream> getResource(PackType packType, ResourceLocation location) {
				return pack.getResource(packType, location.withPrefix(prefix));
			}

			@Override
			public void listResources(PackType packType, String namespace, String path, ResourceOutput resourceOutput) {
				pack.listResources(packType, namespace, prefix+path, (rl, ioSupplier) ->
					resourceOutput.accept(wrapLocation(rl), ioSupplier));
			}

			@Override
			public Set<String> getNamespaces(PackType type) {
				return pack.getNamespaces(type);
			}

			@Nullable @Override
			public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) throws IOException {
				return pack.getMetadataSection(deserializer);
			}

			@Override
			public PackLocationInfo location() {
				return pack.location();
			}

			@Override
			public String packId() {
				return pack.packId();
			}

			@Override
			public void close() {
				pack.close();
			}
		};
	}

	@Override
	public Optional<Resource> getResource(ResourceLocation location) {
		return wrapped.getResource(location.withPrefix(prefix)).map(this::wrapResource);
	}
}
