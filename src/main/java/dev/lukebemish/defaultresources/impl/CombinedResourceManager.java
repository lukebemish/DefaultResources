package dev.lukebemish.defaultresources.impl;

import com.mojang.datafixers.util.Pair;
import dev.lukebemish.defaultresources.api.GlobalResourceManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.jspecify.annotations.NonNull;

public class CombinedResourceManager implements GlobalResourceManager {
	private final List<PackResources> resources;
	private final Map<String, List<PackResources>> namespaceMap;
	private final PackType type;

	public CombinedResourceManager(PackType type, List<Pair<String, Pack.ResourcesSupplier>> resources) {
		this.type = type;
		this.resources = resources.stream().sorted(Comparator.comparing(Pair::getFirst)).map(p -> p.getSecond().openPrimary(DefaultResources.infoFor(p.getFirst()))).toList();
		Map<String, List<PackResources>> namespaceBuilder = new HashMap<>();
		for (var resource : this.resources) {
			for (var namespace : resource.getNamespaces(type)) {
				namespaceBuilder.computeIfAbsent(namespace, k -> new ArrayList<>()).add(resource);
			}
		}
		this.namespaceMap = namespaceBuilder.entrySet().stream().collect(Collectors.toMap(
			Map.Entry::getKey,
			e -> List.copyOf(e.getValue())
		));
	}

	@Override
	public Set<String> getNamespaces() {
		return namespaceMap.keySet();
	}

	@Override
	public List<Resource> getResourceStack(ResourceLocation location) {
		List<Resource> builder = new ArrayList<>();
		getResourceStream(location)
			.forEach(builder::add);
		Collections.reverse(builder);
		return Collections.unmodifiableList(builder);
	}

	private Stream<Resource> getResourceStream(ResourceLocation location) {
		return namespaceMap.getOrDefault(location.getNamespace(), List.of()).stream()
			.map(p -> {
				IoSupplier<InputStream> ioSupplier = p.getResource(this.type, location);
				if (ioSupplier != null) {
					ResourceLocation metadataLocation = getMetadataLocation(location);
					IoSupplier<ResourceMetadata> metadataSupplier = () -> {
						IoSupplier<InputStream> metadataIoSupplier = p.getResource(this.type, metadataLocation);
						if (metadataIoSupplier == null) return ResourceMetadata.EMPTY;
						try (InputStream metadata = ioSupplier.get()) {
							return ResourceMetadata.fromJsonStream(metadata);
						} catch (Throwable t) {
							throw new IOException(t);
						}
					};
					return new Resource(p, ioSupplier, metadataSupplier);
				}
				return null;
			})
			.filter(Objects::nonNull);
	}

	private static ResourceLocation getMetadataLocation(ResourceLocation location) {
		return location.withPath(location.getPath() + ".mcmeta");
	}

	@Override
	public Map<ResourceLocation, Resource> listResources(String path, Predicate<ResourceLocation> filter) {
		Map<ResourceLocation, Resource> builder = new HashMap<>();
		BiConsumer<ResourceLocation, Resource> consumer = builder::put;
		findResources(path, filter, consumer);
		return Collections.unmodifiableMap(builder);
	}

	@Override
	public Map<ResourceLocation, List<Resource>> listResourceStacks(String path, Predicate<ResourceLocation> filter) {
		Map<ResourceLocation, List<Resource>> builder = new HashMap<>();
		BiConsumer<ResourceLocation, Resource> consumer = (rl, r) -> builder.computeIfAbsent(rl, k -> new ArrayList<>()).add(r);
		findResources(path, filter, consumer);
		return builder.entrySet().stream().collect(Collectors.toMap(
			Map.Entry::getKey,
			e -> {
				var list = e.getValue();
				Collections.reverse(list);
				return Collections.unmodifiableList(list);
			}
		));
	}

	private void findResources(String path, Predicate<ResourceLocation> filter, BiConsumer<ResourceLocation, Resource> consumer) {
		for (Map.Entry<String, List<PackResources>> entry : namespaceMap.entrySet()) {
			var namespace = entry.getKey();
			for (PackResources packResources : entry.getValue()) {
				packResources.listResources(this.type, namespace, path, (rl, ioSupplier) -> {
					if (rl.getPath().endsWith(".mcmeta")) return;
					if (filter.test(rl)) {
						ResourceLocation metadataLocation = getMetadataLocation(rl);
						IoSupplier<ResourceMetadata> metadataSupplier = () -> {
							IoSupplier<InputStream> metadataIoSupplier = packResources.getResource(this.type, metadataLocation);
							if (metadataIoSupplier == null) return ResourceMetadata.EMPTY;
							try (InputStream metadata = ioSupplier.get()){
								return ResourceMetadata.fromJsonStream(metadata);
							} catch (Throwable t) {
								throw new IOException(t);
							}
						};
						consumer.accept(rl, new Resource(packResources, ioSupplier, metadataSupplier));
					}
				});
			}
		}
	}

	@Override
	public Stream<PackResources> listPacks() {
		return resources.stream();
	}

	@Override
	public Optional<Resource> getResource(ResourceLocation location) {
		AtomicReference<Resource> resource = new AtomicReference<>();
		getResourceStream(location)
			.forEach(resource::set);
		return Optional.ofNullable(resource.get());
	}
}
