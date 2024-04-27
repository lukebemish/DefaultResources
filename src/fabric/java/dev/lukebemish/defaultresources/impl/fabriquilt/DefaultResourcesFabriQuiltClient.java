package dev.lukebemish.defaultresources.impl.fabriquilt;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.server.packs.PackType;

@SuppressWarnings("deprecation")
public class DefaultResourcesFabriQuiltClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		DefaultResourcesFabriQuilt.addPackResources(PackType.CLIENT_RESOURCES);
	}
}
