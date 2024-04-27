package dev.lukebemish.defaultresources.api;

import dev.lukebemish.defaultresources.impl.ManagerHolder;
import dev.lukebemish.defaultresources.impl.Services;
import dev.lukebemish.defaultresources.impl.WrappingResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;

/**
 * A resource manager that can be used to load resources from global (static) resources.
 */
public interface GlobalResourceManager extends ResourceManager {

	/**
	 * Provides a resource manager for global assets. Global assets are data that is not specific to a world, and is
	 * required only on the client. The acquired resource manager should be made more specific with {@link #prefix(String)}.
	 * @return a resource manager which loads resources in {@code globalassets}
	 * @throws IllegalStateException if called on a dedicated server
	 */
	@Contract(pure = true)
	static GlobalResourceManager getGlobalAssets() {
		if (!Services.PLATFORM.isClient()) {
			throw new IllegalStateException("Cannot create client resource manager on dedicated server");
		}
		return ManagerHolder.STATIC_ASSETS;
	}

	/**
	 * Provides a resource manager for global data. Global data is data that is not specific to a world, and is required
	 * on the server. The acquired resource manager should be made more specific with {@link #prefix(String)}.
	 * @return a resource manager which loads resources in {@code globaldata}
	 */
	@Contract(pure = true)
	static GlobalResourceManager getGlobalData() {
		return ManagerHolder.STATIC_DATA;
	}

	/**
	 * Creates a new global resource manager that looks are resources in the given prefix, relative to the current path.
	 * @param prefix the prefix to use for the resource manager
	 * @return a new resource manager, which loads resources in {@code global[data/assets]/[namespace]/[original path]/[prefix]}.
	 */
	@Contract(value = "_ -> new", pure = true)
	@ApiStatus.NonExtendable
	default GlobalResourceManager prefix(String prefix) {
		return new WrappingResourceManager(this, prefix);
	}
}
