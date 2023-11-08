package net.querz.mcaselector.version.anvil117;

import net.querz.mcaselector.math.MathUtil;
import net.querz.mcaselector.realshading.ShadeConstants;
import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.ui.Color;
import net.querz.mcaselector.version.ChunkRenderer;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.regex.RegexMapping;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Anvil117ChunkRenderer implements ChunkRenderer {
	@Override
	public void drawChunk(CompoundTag root, ColorMapping colorMapping, int x, int z, int scale, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, boolean water, boolean applyBiomeTint, int height) {
		ListTag sections = Helper.getSectionsFromLevelFromRoot(root, "Sections");
		if (sections == null) {
			return;
		}

		CompoundTag level = Helper.tagFromCompound(root, "Level");

		int absHeight = height + 64;

		ListTag[] palettes = new ListTag[24];
		long[][] blockStatesArray = new long[24][];
		sections.forEach(s -> {
			ListTag p = Helper.tagFromCompound(s, "Palette");
			long[] b = Helper.longArrayFromCompound(s, "BlockStates");
			int y = Helper.numberFromCompound(s, "Y", -5).intValue();
			if (y >= -4 && y <= 19 && p != null && b != null) {
				palettes[y + 4] = p;
				blockStatesArray[y + 4] = b;
			}
		});

		int[] biomes = Helper.intArrayFromCompound(level, "Biomes");

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx += scale) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz += scale) {

				// loop over sections
				boolean waterDepth = false;
				for (int i = palettes.length - (24 - (absHeight >> 4)); i >= 0; i--) {
					if (blockStatesArray[i] == null) {
						continue;
					}

					long[] blockStates = blockStatesArray[i];
					ListTag palette = palettes[i];

					int sectionHeight = (i - 4) * Tile.CHUNK_SIZE;

					int bits = blockStates.length >> 6;
					int clean = ((int) Math.pow(2, bits) - 1);

					int startHeight;
					if (absHeight >> 4 == i) {
						startHeight = Tile.CHUNK_SIZE - (16 - absHeight % 16);
					} else {
						startHeight = Tile.CHUNK_SIZE - 1;
					}

					for (int cy = startHeight; cy >= 0; cy--) {
						int paletteIndex = getPaletteIndex(getIndex(cx, cy, cz), blockStates, bits, clean);
						CompoundTag blockData = palette.getCompound(paletteIndex);

						if (isEmpty(blockData)) {
							continue;
						}

						int biome = getBiomeAtBlock(biomes, cx, sectionHeight + cy, cz);
						biome = MathUtil.clamp(biome, 0, 255);
						if(!applyBiomeTint) biome = ColorMapping.DEFAULT_BIOME;

						int regionIndex = ((z + cz / scale) * (Tile.SIZE / scale) + (x + cx / scale));
						if (water) {
							if (!waterDepth) {
								pixelBuffer[regionIndex] = colorMapping.getRGB(blockData, biome); // water color
								waterHeights[regionIndex] = (short) (sectionHeight + cy); // height of highest water or terrain block
							}
							if (isWater(blockData)) {
								waterDepth = true;
								continue;
							} else if (isWaterlogged(blockData)) {
								pixelBuffer[regionIndex] = colorMapping.getRGB(waterDummy, biome); // water color
								waterPixels[regionIndex] = colorMapping.getRGB(blockData, biome); // color of waterlogged block
								waterHeights[regionIndex] = (short) (sectionHeight + cy);
								terrainHeights[regionIndex] = (short) (sectionHeight + cy - 1); // "height" of bottom of water, which will just be 1 block lower so shading works
								continue zLoop;
							} else {
								waterPixels[regionIndex] = colorMapping.getRGB(blockData, biome); // color of block at bottom of water
							}
						} else {
							pixelBuffer[regionIndex] = colorMapping.getRGB(blockData, biome);
						}
						terrainHeights[regionIndex] = (short) (sectionHeight + cy); // height of bottom of water
						continue zLoop;
					}
				}
			}
		}
	}

	@Override
	public void drawRegex(CompoundTag root, ColorMapping colorMapping, RegexMapping regexMapping, Pattern regexPattern, String displayGroup, int x, int z, int scale, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, boolean water, boolean applyBiomeTint, int height) {
		ListTag sections = Helper.getSectionsFromLevelFromRoot(root, "Sections");
		if (sections == null) {
			return;
		}

		CompoundTag level = Helper.tagFromCompound(root, "Level");

		int absHeight = height + 64;

		ListTag[] palettes = new ListTag[24];
		long[][] blockStatesArray = new long[24][];
		sections.forEach(s -> {
			ListTag p = Helper.tagFromCompound(s, "Palette");
			long[] b = Helper.longArrayFromCompound(s, "BlockStates");
			int y = Helper.numberFromCompound(s, "Y", -5).intValue();
			if (y >= -4 && y <= 19 && p != null && b != null) {
				palettes[y + 4] = p;
				blockStatesArray[y + 4] = b;
			}
		});

		int[] biomes = Helper.intArrayFromCompound(level, "Biomes");

		final Matcher matcher = regexPattern.matcher("");
		StringBuilder input = new StringBuilder();
		String emptyCollection = Character.toString(regexMapping.encode("air")).repeat(Tile.CHUNK_SIZE);

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx += scale) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz += scale) {

				int regionIndex = ((z + cz / scale) * (Tile.SIZE / scale) + (x + cx / scale));
				boolean start = true;
				int startSkippedBlocks = 0;
				input.setLength(0);

				int i = -1, starti = palettes.length - (24 - (absHeight >> 4));
				for (i = starti; i >= 0; i--) {
					if (blockStatesArray[i] == null) {
						if(start) {
							startSkippedBlocks += Tile.CHUNK_SIZE;
						}
						input.append(emptyCollection);
						continue;
					}
					start = false;

					long[] blockStates = blockStatesArray[i];
					ListTag palette = palettes[i];

					int bits = blockStates.length >> 6;
					int clean = ((int) Math.pow(2, bits) - 1);
					int startHeight;
					if (absHeight >> 4 == i) {
						startHeight = Tile.CHUNK_SIZE - (16 - absHeight % 16);
					} else {
						startHeight = Tile.CHUNK_SIZE - 1;
					}

					for (int cy = startHeight; cy >= 0; cy--) {
						int paletteIndex = getPaletteIndex(getIndex(cx, cy, cz), blockStates, bits, clean);
						CompoundTag blockData = palette.getCompound(paletteIndex);

						String block = Helper.stringFromCompound(blockData, "Name", "");
						input.append(regexMapping.encode(block));
					}
				}

				boolean foundMatch = false;
				matcher.reset(input);
				while (matcher.find()) {
					if (!matcher.group(displayGroup).isEmpty()) {
						foundMatch = true;
						break;
					}
				}
				if (!foundMatch) continue zLoop;



				int index = /*skippedBlocks +*/ matcher.start(displayGroup);
				if(index < startSkippedBlocks) continue zLoop;
				i = starti - index / Tile.CHUNK_SIZE;
				int indexCY = Tile.CHUNK_SIZE - index % Tile.CHUNK_SIZE - 1;
				CompoundTag blockData;
				int biome = -1;

				{
					ListTag palette = palettes[i];
					long[] blockStates = blockStatesArray[i];
					int bits = blockStates.length >> 6;
					int clean = (int) Math.pow(2, bits) - 1;
					int paletteIndex = getPaletteIndex(getIndex(cx, indexCY, cz), blockStates, bits, clean);
					blockData = code(palette.getCompound(paletteIndex), regexMapping);
					if(isEmpty(blockData)) continue zLoop;

					biome = getBiomeAtBlock(biomes, cx, height - index, cz);
					biome = MathUtil.clamp(biome, 0, 255);
				}
				pixelBuffer[regionIndex] = getColor(colorMapping, regexMapping, blockData, biome, applyBiomeTint);
				if (water) waterPixels[regionIndex] = pixelBuffer[regionIndex];
				terrainHeights[regionIndex] = (short) (height - index);
				if (water) waterHeights[regionIndex] = terrainHeights[regionIndex];

				if (water && isWater(blockData)) {
					for (int j = i; j >= 0; j--) {
						if (blockStatesArray[j] == null) {
							continue;
						}

						long[] blockStates = blockStatesArray[j];
						ListTag palette = palettes[j];

						int sectionHeight = (j - 4) * Tile.CHUNK_SIZE;

						int bits = blockStates.length >> 6;
						int clean = ((int) Math.pow(2, bits) - 1);

						int startHeight;
						if(j == i){
							startHeight = indexCY;
						} else if (absHeight >> 4 == j) {
							startHeight = Tile.CHUNK_SIZE - (16 - absHeight % 16);
						} else {
							startHeight = Tile.CHUNK_SIZE - 1;
						}

						for (int cy = startHeight; cy >= 0; cy--) {
							int paletteIndex = getPaletteIndex(getIndex(cx, cy, cz), blockStates, bits, clean);
							CompoundTag wblockData = code(palette.getCompound(paletteIndex), regexMapping);

							if (isWater(wblockData)) {
								continue;
							}

							int wbiome = getBiomeAtBlock(biomes, cx, sectionHeight + cy, cz);
							wbiome = MathUtil.clamp(wbiome, 0, 255);

							if (isWaterlogged(wblockData)) {
								pixelBuffer[regionIndex] = getColor(colorMapping, regexMapping, waterDummy, wbiome, applyBiomeTint); // water color
								waterPixels[regionIndex] = getColor(colorMapping, regexMapping, wblockData, wbiome, applyBiomeTint); // color of waterlogged block
								waterHeights[regionIndex] = (short) (sectionHeight + cy);
								terrainHeights[regionIndex] = (short) (sectionHeight + cy - 1); // "height" of bottom of water, which will just be 1 block lower so shading works
							} else {
								waterPixels[regionIndex] = getColor(colorMapping, regexMapping, wblockData, wbiome, applyBiomeTint); // color of block at bottom of water
								terrainHeights[regionIndex] = (short) (sectionHeight + cy);
							}

							continue zLoop;
						}
					}
				}
			}
		}
	}

	@Override
	public void drawShade2(CompoundTag root, ColorMapping colorMapping, int x, int z, int scale, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, boolean[] shades, int x0, int z0, boolean water, boolean applyBiomeTint, int height) {
		ListTag sections = Helper.getSectionsFromLevelFromRoot(root, "Sections");
		if (sections == null) {
			return;
		}
		CompoundTag level = Helper.tagFromCompound(root, "Level");

		final int SHADEX = ShadeConstants.GLOBAL.rX	* 512, SHADEZ = ShadeConstants.GLOBAL.rZ * 512;

		int absHeight = height + 64;

		ListTag[] palettes = new ListTag[24];
		long[][] blockStatesArray = new long[24][];
		sections.forEach(s -> {
			ListTag p = Helper.tagFromCompound(s, "Palette");
			long[] b = Helper.longArrayFromCompound(s, "BlockStates");
			int y = Helper.numberFromCompound(s, "Y", -5).intValue();
			if (y >= -4 && y <= 19 && p != null && b != null) {
				palettes[y + 4] = p;
				blockStatesArray[y + 4] = b;
			}
		});

		int[] biomes = Helper.intArrayFromCompound(level, "Biomes");

		for (int _cz = 0; _cz < Tile.CHUNK_SIZE; _cz += scale) {
			int cz = ShadeConstants.GLOBAL.flowZ(_cz, 0, Tile.CHUNK_SIZE);
			for (int _cx = 0; _cx < Tile.CHUNK_SIZE; _cx += scale) {
				int cx = ShadeConstants.GLOBAL.flowX(_cx, 0, Tile.CHUNK_SIZE);

				int regionIndex = ((z + cz / scale) * (Tile.SIZE / scale) + (x + cx / scale));
				// loop over sections

				boolean done = false, waterDepth = false;
				int waterShade = 0, hw = 0;

				int starti = palettes.length - (24 - (absHeight >> 4));
				for (int i = starti; i >= 0; i--) {
					if (blockStatesArray[i] == null) {
						continue;
					}

					long[] blockStates = blockStatesArray[i];
					ListTag palette = palettes[i];

					int sectionHeight = (i - 4) * Tile.CHUNK_SIZE;

					int bits = blockStates.length >> 6;
					int clean = ((int) Math.pow(2, bits) - 1);

					int startHeight;
					if (absHeight >> 4 == i) {
						startHeight = Tile.CHUNK_SIZE - (16 - absHeight % 16);
					} else {
						startHeight = Tile.CHUNK_SIZE - 1;
					}

					for (int cy = startHeight; cy >= 0; cy--) {
						int paletteIndex = getPaletteIndex(getIndex(cx, cy, cz), blockStates, bits, clean);
						CompoundTag blockData = palette.getCompound(paletteIndex);
						String blockName = Helper.stringFromCompound(blockData, "Name", "");
						boolean isEmpty = isEmpty(blockData), isWater = isWater(blockData), isWaterLogged = isWaterlogged(blockData);
						boolean isSolid = !isEmpty && !isWater && !isWaterLogged;

						int h = absHeight - (i * 16 + cy);

						int x1 = (int)Math.floor((x0 + x + cx / scale) + ShadeConstants.GLOBAL.cosAcotgB * h / scale), z1 = (int)Math.floor((z0 + z + cz / scale) + -ShadeConstants.GLOBAL.sinAcotgB * h / scale);
						int x2 = (int)Math.floor(x1 + ShadeConstants.GLOBAL.cosAcotgB / scale), z2 = (int)Math.floor(z1 + -ShadeConstants.GLOBAL.sinAcotgB / scale);



						boolean alreadyshade = checkline(shades, SHADEX, x2, z2, x1, z1);

						if(!isEmpty && !done) {
							int biome = getBiomeAtBlock(biomes, cx, sectionHeight + cy, cz);
							biome = MathUtil.clamp(biome, 0, 255);
							if(!applyBiomeTint) biome = ColorMapping.DEFAULT_BIOME;

							int blockDataColor = colorMapping.getRGB(blockData, biome);
							int intensity = 0;
							if(alreadyshade && !waterDepth){
								intensity = (int)(ShadeConstants.SHADEMOODYNESS * 1);
								if(isWater) {
									waterShade = (int)(0.5 * intensity);
								}
							}
							blockDataColor = Color.shade(blockDataColor, intensity);

							if (water) {
								if (!waterDepth) {
									pixelBuffer[regionIndex] = blockDataColor; // water color
									waterHeights[regionIndex] = (short) (sectionHeight + cy); // height of highest water or terrain block
								}
								if (isWater) {
									waterDepth = true;
									hw++;
								} else{
									if(waterDepth){
										final double waterAbsorbtion = 0.1;
										waterShade = MathUtil.clamp((int)(ShadeConstants.SHADEMOODYNESS * waterAbsorbtion * hw) + waterShade, ShadeConstants.SHADEMOODYNESS, 0);
										pixelBuffer[regionIndex] = Color.shade(colorMapping.getRGB(waterDummy, biome), waterShade); // water color
									}

									if (isWaterLogged) {
										waterPixels[regionIndex] = blockDataColor; // color of waterlogged block
										waterHeights[regionIndex] = (short) (sectionHeight + cy);
										terrainHeights[regionIndex] = (short) (sectionHeight + cy - 1); // "height" of bottom of water, which will just be 1 block lower so shading works
										done = true;
									} else {
										waterPixels[regionIndex] = blockDataColor; // color of block at bottom of water
										terrainHeights[regionIndex] = (short) (sectionHeight + cy);
										done = true;
									}
								}
							} else {
								pixelBuffer[regionIndex] = blockDataColor;
								terrainHeights[regionIndex] = (short) (sectionHeight + cy);
								done = true;
							}

							if(done) waterDepth = false;
						}

						boolean solidness;
						if(isSolid){
							if(blockName.contains("_leaves")){
								solidness = true;
							} else if(blockName.contains("stained_glass") || blockName.equals("minecraft:tinted_glass")) {
								solidness = true;
							} else if(blockName.equals("minecraft:grass") || blockName.equals("minecraft:tall_grass")) {
								solidness = false;
							} else {
								solidness = true;
							}
						} else {
							solidness = false;
						}

						if(!alreadyshade && solidness) {
							//int xp = -1, zp = -1;
							setline(shades, solidness, SHADEX, x2, z2, x1, z1);
						}

					}
				}



			}
		}
	}

	@Override
	public void drawBiomes(CompoundTag root, ColorMapping colorMapping, int x, int z, int scale, int[] pixelBuffer, int height){
		ListTag sections = Helper.getSectionsFromLevelFromRoot(root, "Sections");
		if (sections == null) {
			return;
		}

		CompoundTag level = Helper.tagFromCompound(root, "Level");

		int absHeight = height + 64;

		ListTag[] palettes = new ListTag[24];
		long[][] blockStatesArray = new long[24][];
		sections.forEach(s -> {
			ListTag p = Helper.tagFromCompound(s, "Palette");
			long[] b = Helper.longArrayFromCompound(s, "BlockStates");
			int y = Helper.numberFromCompound(s, "Y", -5).intValue();
			if (y >= -4 && y <= 19 && p != null && b != null) {
				palettes[y + 4] = p;
				blockStatesArray[y + 4] = b;
			}
		});

		int[] biomes = Helper.intArrayFromCompound(level, "Biomes");

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx += scale) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz += scale) {

				for (int i = palettes.length - (24 - (absHeight >> 4)); i >= 0; i--) {
					if (blockStatesArray[i] == null) {
						continue;
					}

					long[] blockStates = blockStatesArray[i];
					ListTag palette = palettes[i];

					int sectionHeight = (i - 4) * Tile.CHUNK_SIZE;

					int bits = blockStates.length >> 6;
					int clean = ((int) Math.pow(2, bits) - 1);

					int startHeight;
					if (absHeight >> 4 == i) {
						startHeight = Tile.CHUNK_SIZE - (16 - absHeight % 16);
					} else {
						startHeight = Tile.CHUNK_SIZE - 1;
					}

					for (int cy = startHeight; cy >= 0; cy--) {
						int paletteIndex = getPaletteIndex(getIndex(cx, cy, cz), blockStates, bits, clean);
						CompoundTag blockData = palette.getCompound(paletteIndex);

						if (isEmpty(blockData)) {
							continue;
						}

						int biome = getBiomeAtBlock(biomes, cx, sectionHeight + cy, cz);
						biome = MathUtil.clamp(biome, 0, 255);

						int regionIndex = ((z + cz / scale) * (Tile.SIZE / scale) + (x + cx / scale));
						pixelBuffer[regionIndex] = colorMapping.getBiomeColor(biome);

						continue zLoop;
					}
				}
			}
		}
	}

	@Override
	public void drawLayer(CompoundTag root, ColorMapping colorMapping, int x, int z, int scale, int[] pixelBuffer, int height) {
		ListTag sections = Helper.getSectionsFromLevelFromRoot(root, "Sections");
		if (sections == null) {
			return;
		}

		CompoundTag level = Helper.tagFromCompound(root, "Level");

		CompoundTag section = null;
		for (CompoundTag s : sections.iterateType(CompoundTag.class)) {
			int y = Helper.numberFromCompound(s, "Y", -5).intValue();
			if (y == height >> 4) {
				section = s;
				break;
			}
		}
		if (section == null) {
			return;
		}

		ListTag palette = Helper.tagFromCompound(section, "Palette");
		long[] blockStates = Helper.longArrayFromCompound(section, "BlockStates");
		if (blockStates == null || palette == null) {
			return;
		}

		int[] biomes = Helper.intArrayFromCompound(level, "Biomes");

		int cy = height % 16;
		int bits = blockStates.length >> 6;
		int clean = ((int) Math.pow(2, bits) - 1);

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx += scale) {
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz += scale) {
				int paletteIndex = getPaletteIndex(getIndex(cx, cy, cz), blockStates, bits, clean);
				CompoundTag blockData = palette.getCompound(paletteIndex);

				if (isEmpty(blockData)) {
					continue;
				}

				int biome = getBiomeAtBlock(biomes, cx, height, cz);
				biome = MathUtil.clamp(biome, 0, 255);

				int regionIndex = (z + cz / scale) * (Tile.SIZE / scale) + (x + cx / scale);
				pixelBuffer[regionIndex] = colorMapping.getRGB(blockData, biome);
			}
		}
	}

	@Override
	public void drawCaves(CompoundTag root, ColorMapping colorMapping, int x, int z, int scale, int[] pixelBuffer, short[] terrainHeights, int height) {
		ListTag sections = Helper.getSectionsFromLevelFromRoot(root, "Sections");
		if (sections == null) {
			return;
		}

		CompoundTag level = Helper.tagFromCompound(root, "Level");

		int absHeight = height + 64;

		ListTag[] palettes = new ListTag[24];
		long[][] blockStatesArray = new long[24][];
		sections.forEach(s -> {
			ListTag p = Helper.tagFromCompound(s, "Palette");
			long[] b = Helper.longArrayFromCompound(s, "BlockStates");
			int y = Helper.numberFromCompound(s, "Y", -5).intValue();
			if (y >= -4 && y <= 19 && p != null && b != null) {
				palettes[y + 4] = p;
				blockStatesArray[y + 4] = b;
			}
		});

		int[] biomes = Helper.intArrayFromCompound(level, "Biomes");

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx += scale) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz += scale) {

				int ignored = 0;
				boolean doneSkipping = false;

				// loop over sections
				for (int i = palettes.length - (24 - (absHeight >> 4)); i >= 0; i--) {
					if (blockStatesArray[i] == null) {
						continue;
					}

					long[] blockStates = blockStatesArray[i];
					ListTag palette = palettes[i];

					int sectionHeight = (i - 4) * Tile.CHUNK_SIZE;

					int bits = blockStates.length >> 6;
					int clean = ((int) Math.pow(2, bits) - 1);

					int startHeight;
					if (absHeight >> 4 == i) {
						startHeight = Tile.CHUNK_SIZE - (16 - absHeight % 16);
					} else {
						startHeight = Tile.CHUNK_SIZE - 1;
					}

					for (int cy = startHeight; cy >= 0; cy--) {
						int paletteIndex = getPaletteIndex(getIndex(cx, cy, cz), blockStates, bits, clean);
						CompoundTag blockData = palette.getCompound(paletteIndex);

						if (!isEmptyOrFoliage(blockData, colorMapping)) {
							if (doneSkipping) {
								int regionIndex = (z + cz / scale) * (Tile.SIZE / scale) + (x + cx / scale);
								int biome = getBiomeAtBlock(biomes, cx, sectionHeight + cy, cz);
								biome = MathUtil.clamp(biome, 0, 255);
								pixelBuffer[regionIndex] = colorMapping.getRGB(blockData, biome);
								terrainHeights[regionIndex] = (short) (sectionHeight + cy);
								continue zLoop;
							}
							ignored++;
						} else if (ignored > 0) {
							doneSkipping = true;
						}
					}
				}
			}
		}
	}


	private void setline_(boolean[] shades, boolean value, int SHADEX, int x1, int z1, int x2, int z2) {
		int deltax = Math.abs(x2 - x1);
		int deltaz = Math.abs(z2 - z1);
		int error = 0;
		int zz = z1;

		for(int xx = x1; xx <= x2; xx++) {
			int indx = zz * SHADEX + xx;
			if(indx >= 0 && indx < shades.length) {
				shades[indx] = value;
			}

			error = error + deltaz;
			if(2 * error >= deltax) {
				zz = zz + 1;
				error= error - deltax;
			}
		}
	}
	boolean checkline_(boolean[] shades, int SHADEX, int x1, int z1, int x2, int z2){
		int deltax = Math.abs(x2 - x1);
		int deltaz = Math.abs(z2 - z1);
		int error = 0;
		int br = 0;
		int zz = z1;

		boolean min = true;

		for(int xx = x1; xx <= x2; xx++) {
			int indx = zz * SHADEX + xx;
			if(indx >= 0 && indx < shades.length) {
				br++;
				if (Boolean.compare(min, shades[indx]) > 0) {
					min = shades[indx];
					break;
				}
			}

			//if(shades[(int)Math.ceil(zz) * SHADEX + (int)Math.floor(xx)] == false){
			//	return true;
			//}
			//if(shades[(int)Math.floor(zz) * SHADEX + (int)Math.ceil(xx)] == false){
			//	return true;
			//}
			error = error + deltaz;
			if(2 * error >= deltax) {
				zz = zz + 1;
				error -= deltax;
			}
		}

		return min;
	}



	void setline(boolean[] shades, boolean value, int SHADEX, int x1, int z1, int x2, int z2) {
		int dx = Math.abs(x2 - x1);
		int dz = Math.abs(z2 - z1);
		int sx = x1 < x2 ? 1 : -1;
		int sz = z1 < z2 ? 1 : -1;
		int err = dx - dz;

		for(int n = 1 + dx + dz; n > 0; n--) {
			int indx = z1 * SHADEX + x1;
			if(indx >= 0 && indx < shades.length) {
				shades[indx] = value;
			}

			if (err > 0)
			{
				x1 += sx;
				err -= dz;
			}
			else
			{
				z1 += sz;
				err += dx;
			}
		}

		//points.add(new Point(x2, z2)); // Include the last point
	}

	boolean checkline(boolean[] shades, int SHADEX, int x1, int z1, int x2, int z2) {
		int dx = Math.abs(x2 - x1);
		int dz = Math.abs(z2 - z1);
		int sx = x1 < x2 ? 1 : -1;
		int sz = z1 < z2 ? 1 : -1;
		int err = dx - dz;

		boolean min = true;

		for(int n = 1 + dx + dz; n > 0; n--) {
			int indx = (int)z1 * SHADEX + (int)x1;
			if(indx >= 0 && indx < shades.length) {
				if (Boolean.compare(min, shades[indx]) > 0) {
					min = shades[indx];
					break;
				}
			}

			if (err > 0)
			{
				x1 += sx;
				err -= dz;
			}
			else
			{
				z1 += sz;
				err += dx;
			}
		}

		return min;
		//points.add(new Point(x2, z2)); // Include the last point
	}


	@Override
	public CompoundTag minimizeChunk(CompoundTag root) {
		CompoundTag minData = new CompoundTag();
		minData.put("DataVersion", root.get("DataVersion").copy());
		CompoundTag level = new CompoundTag();
		minData.put("Level", level);
		level.put("Biomes", root.getCompound("Level").get("Biomes").copy());
		level.put("Sections", root.getCompound("Level").get("Sections").copy());
		level.put("Status", root.getCompound("Level").get("Status").copy());
		return minData;
	}

	private static final CompoundTag waterDummy = new CompoundTag();

	static {
		waterDummy.putString("Name", "minecraft:water");
	}

	private boolean isWater(CompoundTag blockData) {
		return switch (Helper.stringFromCompound(blockData, "Name", "")) {
			case "minecraft:water", "minecraft:bubble_column" -> true;
			default -> false;
		};
	}

	private boolean isWaterlogged(CompoundTag data) {
		return data.get("Properties") != null && "true".equals(Helper.stringFromCompound(Helper.tagFromCompound(data, "Properties"), "waterlogged", null));
	}

	private boolean isEmpty(CompoundTag blockData) {
		return switch (Helper.stringFromCompound(blockData, "Name", "")) {
			case "minecraft:air", "minecraft:cave_air", "minecraft:barrier", "minecraft:structure_void", "minecraft:light" -> blockData.size() == 1;
			default -> false;
		};
	}

	private boolean isEmptyOrFoliage(CompoundTag blockData, ColorMapping colorMapping) {
		String name;
		return switch (name = Helper.stringFromCompound(blockData, "Name", "")) {
			case "minecraft:air", "minecraft:cave_air", "minecraft:barrier", "minecraft:structure_void", "minecraft:light", "minecraft:snow" -> blockData.size() == 1;
			default -> colorMapping.isFoliage(name);
		};
	}

	private int getIndex(int x, int y, int z) {
		return y * Tile.CHUNK_SIZE * Tile.CHUNK_SIZE + z * Tile.CHUNK_SIZE + x;
	}

	private int getBiomeIndex(int x, int y, int z) {
		return y * Tile.CHUNK_SIZE + z * 4 + x;
	}

	private int getBiomeAtBlock(int[] biomes, int biomeX, int biomeY, int biomeZ) {
		if (biomes == null) {
			return -1;
		}
		if (biomes.length == 1536) {
			biomeY += 64; // adjust for negative y block coordinates
		} else if (biomes.length != 1024) { // still support 256 height
			return -1;
		}
		return biomes[getBiomeIndex(biomeX / 4, biomeY / 4, biomeZ / 4)];
	}

	private int getPaletteIndex(int index, long[] blockStates, int bits, int clean) {
		int indicesPerLong = (int) (64D / bits);
		int blockStatesIndex = index / indicesPerLong;
		int startBit = (index % indicesPerLong) * bits;
		return (int) (blockStates[blockStatesIndex] >> startBit) & clean;
	}

	private int getColor(ColorMapping colorMapping, RegexMapping regexMapping, CompoundTag blockData, int biome, boolean applyBiomeTint){
		String name = blockData.getString("Name");
		int color;
		if(regexMapping != null){
			color = regexMapping.colorcode(regexMapping.encode(name));
			if(color == Integer.MIN_VALUE) color = colorMapping.getOnlyRGB(blockData);
		} else {
			color = colorMapping.getOnlyRGB(blockData);
		}
		if(applyBiomeTint) color = colorMapping.applyBiomeTint(name, biome, color);
		return color;
	}
	private CompoundTag code(CompoundTag blockData, RegexMapping regexMapping){
		String name = blockData.getString("Name"), mapping = regexMapping.code(name);
		CompoundTag newBlockData = blockData;
		if(!name.equals(mapping)) {
			newBlockData = blockData.copy();
			newBlockData.putString("Name", mapping);
		}
		return newBlockData;
	}
}