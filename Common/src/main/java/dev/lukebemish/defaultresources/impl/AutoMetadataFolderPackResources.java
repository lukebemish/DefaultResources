/*
 * Copyright (C) 2023 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl;

import com.google.gson.JsonObject;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class AutoMetadataFolderPackResources extends PathPackResources {

    private final PackType packType;

    public AutoMetadataFolderPackResources(String s, PackType packType, Path path) {
        super(s, path, false);
        this.packType = packType;
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
}