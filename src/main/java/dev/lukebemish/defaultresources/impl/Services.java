package dev.lukebemish.defaultresources.impl;

import dev.lukebemish.defaultresources.impl.services.Platform;
import java.util.ServiceLoader;

public class Services {
	public static final Platform PLATFORM = load(Platform.class);

	public static <T> T load(Class<T> clazz) {

		final T loadedService = ServiceLoader.load(clazz)
			.findFirst()
			.orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
		DefaultResources.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);
		return loadedService;
	}
}
