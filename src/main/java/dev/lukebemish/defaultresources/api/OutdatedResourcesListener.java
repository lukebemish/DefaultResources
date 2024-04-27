package dev.lukebemish.defaultresources.api;

import dev.lukebemish.defaultresources.impl.DefaultResources;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Allows mods to provide custom logic for when the extracted default resources are outdated, but cannot be updated due
 * to having been changed on disk.
 */
public interface OutdatedResourcesListener {

	/**
	 * Called when the listener is fired.
	 * @param oldDataVersion the data version of the outdated resources on disk, or {@code null} if none is present
	 * @param newDataVersion the data version of the new resources the mod provides, or {@code null} if none is present
	 */
	void resourcesOutdated(@Nullable String oldDataVersion, @Nullable String newDataVersion);

	/**
	 * Registers a listener to be notified when the extracted default resources are outdated, but cannot be updated due
	 * to having been changed on disk.
	 * @param modId the mod ID to listen for
	 * @param listener the listener to call if the resources cannot be updated
	 */
	@SuppressWarnings("unused")
	static void register(String modId, OutdatedResourcesListener listener) {
		DefaultResources.delegate(() -> {
			Optional<String> oldVersion = DefaultResources.OUTDATED_TARGETS.get(modId);
			Optional<String> newVersion = DefaultResources.MOD_TARGETS.get(modId);
			if (oldVersion != null && newVersion != null) {
				listener.resourcesOutdated(oldVersion.orElse(null), newVersion.orElse(null));
			}
		}, () -> DefaultResources.addListener(modId, listener));
	}
}
