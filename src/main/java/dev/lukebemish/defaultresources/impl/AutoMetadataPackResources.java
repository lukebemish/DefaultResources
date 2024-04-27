package dev.lukebemish.defaultresources.impl;

import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;

public abstract class AutoMetadataPackResources extends AbstractPackResources {
	private final PackType packType;
	private final String name;

	public AutoMetadataPackResources(PackLocationInfo packLocationInfo, PackType packType, String prefix) {
		super(packLocationInfo);
		this.packType = packType;
		this.name = prefix+packType.getDirectory();
	}

	@Nullable @Override
	public IoSupplier<InputStream> getRootResource(String... elements) {
		return null;
	}

	@Nullable
	@Override
	public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
		if (serializer.getMetadataSectionName().equals("pack")) {
			JsonObject object = new JsonObject();
			object.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(this.packType));
			object.addProperty("description", "Global resources");
			return serializer.fromJson(object);
		}
		return null;
	}

	protected String getPackFolderName() {
		return name;
	}
}
