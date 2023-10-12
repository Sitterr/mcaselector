package net.querz.mcaselector.version;

import net.querz.mcaselector.regex.RegexMapping;
import net.querz.nbt.CompoundTag;

import java.util.regex.Pattern;

public interface ChunkRenderer {

	void drawChunk(CompoundTag root, ColorMapping colorMapping, int x, int z, int scale, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, boolean water, boolean applyBiomeTint, int height);

	default void drawRegex(CompoundTag root, ColorMapping colorMapping, RegexMapping regexMapping, Pattern regexPattern, String displayGroup, int x, int z, int scale, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, boolean water, boolean applyBiomeTint, int height){

	}

	default void drawBiomes(CompoundTag root, ColorMapping colorMapping, int x, int z, int scale, int[] pixelBuffer, int height){

	}

	default void drawShade2(CompoundTag root, ColorMapping colorMapping, int x, int z, int scale, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, boolean[] shades, int x0, int z0, boolean water, boolean applyBiomeTint, int height){

	}


	void drawLayer(CompoundTag root, ColorMapping colorMapping, int x, int z, int scale, int[] pixelBuffer, int height);

	void drawCaves(CompoundTag root, ColorMapping colorMapping, int x, int z, int scale, int[] pixelBuffer, short[] terrainHeights, int height);

	CompoundTag minimizeChunk(CompoundTag root);
}
