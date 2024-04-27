package dev.lukebemish.defaultresources.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record DefaultResourcesMetadataSection(boolean detect) {
	public static final Codec<DefaultResourcesMetadataSection> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.BOOL.optionalFieldOf("detect", false).forGetter(DefaultResourcesMetadataSection::detect)
	).apply(i, DefaultResourcesMetadataSection::new));
}
