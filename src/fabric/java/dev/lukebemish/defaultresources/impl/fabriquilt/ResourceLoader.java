package dev.lukebemish.defaultresources.impl.fabriquilt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;

public class ResourceLoader {
	private ResourceLoader() {}

	public static final ResourceLoader CLIENT = new ResourceLoader();
	public static final ResourceLoader SERVER = new ResourceLoader();

	private final List<Supplier<List<PackResources>>> packs = new ArrayList<>();

	public void addPacks(Supplier<List<PackResources>> resources) {
		this.packs.add(resources);
	}

	public void appendTopPacks(Consumer<PackResources> packConsumer) {
		for (var packSupplier : this.packs) {
			for (var pack : packSupplier.get()) {
				packConsumer.accept(pack);
			}
		}
	}

	public static ResourceLoader get(PackType type) {
		return switch (type) {
			case CLIENT_RESOURCES -> CLIENT;
			case SERVER_DATA -> SERVER;
		};
	}
}
