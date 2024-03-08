/*
 * Copyright (C) 2024 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl;

import dev.lukebemish.defaultresources.api.GlobalResourceManager;
import net.minecraft.server.packs.PackType;

public final class ManagerHolder {
    private ManagerHolder() {}

    public static final GlobalResourceManager STATIC_ASSETS = DefaultResources.createStaticResourceManager(PackType.CLIENT_RESOURCES);
    public static final GlobalResourceManager STATIC_DATA = DefaultResources.createStaticResourceManager(PackType.SERVER_DATA);
}
