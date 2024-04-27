package dev.lukebemish.defaultresources.impl.mixin;

import java.io.InputStream;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Resource.class)
public interface ResourceAccessor {
	@Accessor(value = "streamSupplier")
	IoSupplier<InputStream> defaultresources_getStreamSupplier();
	@Accessor(value = "metadataSupplier")
	IoSupplier<ResourceMetadata> defaultresources_getMetadataSupplier();
}
