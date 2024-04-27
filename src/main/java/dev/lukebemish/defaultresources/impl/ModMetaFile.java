package dev.lukebemish.defaultresources.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

public record ModMetaFile(String resourcesPath, boolean zip, boolean extract, Optional<String> dataVersion) {
	public static final Codec<ModMetaFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.STRING.optionalFieldOf("resources_path", "defaultresources").forGetter(ModMetaFile::resourcesPath),
		Codec.BOOL.optionalFieldOf("zip", true).forGetter(ModMetaFile::zip),
		Codec.BOOL.optionalFieldOf("extract", true).forGetter(ModMetaFile::extract),
		Codec.STRING.optionalFieldOf("data_version").forGetter(ModMetaFile::dataVersion)
	).apply(instance, ModMetaFile::new));
}
