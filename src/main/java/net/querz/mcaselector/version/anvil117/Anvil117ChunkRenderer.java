package net.querz.mcaselector.version.anvil117;

import groovyjarjarantlr4.v4.runtime.atn.PredicateTransition;
import net.querz.mcaselector.Main;
import net.querz.mcaselector.io.registry.BiomeRegistry;
import net.querz.mcaselector.math.MathUtil;
import net.querz.mcaselector.text.TextHelper;
import net.querz.mcaselector.tile.Tile;
import net.querz.mcaselector.version.ChunkRenderer;
import net.querz.mcaselector.version.ColorMapping;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.RegexMapping;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;

import java.util.regex.Matcher;

public class Anvil117ChunkRenderer implements ChunkRenderer {


	//static Matcher matcher = PATTERN.matcher("");

	@Override
	public void drawChunk(CompoundTag root, ColorMapping colorMapping, int x, int z, int scale, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, boolean water, int defaultBiome, int height) {
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

		final Matcher matcher = Main.PATTERN.matcher("");
		String emptyCollection = Character.toString(RegexMapping.encode("air")).repeat(Tile.CHUNK_SIZE);

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx += scale) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz += scale) {

				long now = System.currentTimeMillis();

				int regionIndex = ((z + cz / scale) * (Tile.SIZE / scale) + (x + cx / scale));

				StringBuilder names = new StringBuilder();
				boolean foundMatch = false;
				int skippedBlocks = 0;

				int i = -1;
				for (i = palettes.length - (24 - (absHeight >> 4)); i >= 0; i--) {
					if(foundMatch) break;
					if (blockStatesArray[i] == null) {
						skippedBlocks += Tile.CHUNK_SIZE;
						names.append(emptyCollection);
						continue;
					}

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
						names.append(RegexMapping.encode(block));
					}

					matcher.reset(names);
					while (matcher.find()) {
						if (!matcher.group(Main.GROUP).isEmpty()) {
							foundMatch = true;
							i++;
							break;
						}
					}
				}
				if (!foundMatch) continue;

				Main.timeLoop.addAndGet((int) (System.currentTimeMillis() - now));



				long nowf = System.currentTimeMillis();


				int index = /*skippedBlocks +*/ matcher.start(Main.GROUP);
				if(index < skippedBlocks) continue zLoop;
				int indexCY = Tile.CHUNK_SIZE - index % Tile.CHUNK_SIZE - 1;
				CompoundTag blockData;
				int biome = -1;

				{
					ListTag palette = palettes[i];
					long[] blockStates = blockStatesArray[i];
					int bits = blockStates.length >> 6;
					int clean = (int) Math.pow(2, bits) - 1;
					int paletteIndex = getPaletteIndex(getIndex(cx, indexCY, cz), blockStates, bits, clean);
					blockData = palette.getCompound(paletteIndex);
					String name = blockData.getString("Name");
					//blockData = new CompoundTag();
					blockData.putString("Name", RegexMapping.code(name));
					if(isEmpty(blockData)) continue zLoop;

					biome = getBiomeAtBlock(biomes, cx, height - index, cz);
					biome = MathUtil.clamp(biome, 0, 255);
					biome = defaultBiome == -1 ? biome : defaultBiome;
				}
				pixelBuffer[regionIndex] = colorMapping.getRGB(blockData, biome);
				if (water) waterPixels[regionIndex] = pixelBuffer[regionIndex];
				terrainHeights[regionIndex] = (short) (height - index);
				if (water) waterHeights[regionIndex] = terrainHeights[regionIndex];

				/*int color;
				int mappedColor = RegexMapping.colorcode(switch (Main.PROPERTY){
					case "BLOCK" -> Helper.stringFromCompound(blockData, "Name", "");
					case "BIOME" -> BiomeRegistry.toName(biome);
					default -> "";
				});
				color = mappedColor;
				if(color == Integer.MIN_VALUE) {
					int defColor = switch (Main.PROPERTY){
						case "BLOCK" -> colorMapping.getRGB(blockData, biome);
						case "BIOME" -> Integer.MIN_VALUE;
						default -> Integer.MIN_VALUE;
					};
					color = defColor;
				}
				if(color == Integer.MIN_VALUE) continue;

				if(Main.PROPERTY.equals("BLOCK")){
					terrainHeights[regionIndex] = (short) (height - index);
					if (water) waterHeights[regionIndex] = terrainHeights[regionIndex];

					//color = colorMapping.applyBiomeTint(Helper.stringFromCompound(blockData, "Name", ""), biome, color);
				}

				pixelBuffer[regionIndex] = color;
				if (water) waterPixels[regionIndex] = pixelBuffer[regionIndex];*/


				Main.timeLogic.addAndGet((int) (System.currentTimeMillis() - nowf));

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
							CompoundTag wblockData = palette.getCompound(paletteIndex);
							wblockData.putString("Name", RegexMapping.code(wblockData.getString("Name")));

							if (isWater(wblockData)) {
								continue;
							}

							int wbiome = getBiomeAtBlock(biomes, cx, sectionHeight + cy, cz);
							wbiome = MathUtil.clamp(wbiome, 0, 255);

							if (isWaterlogged(wblockData)) {
								pixelBuffer[regionIndex] = colorMapping.getRGB(waterDummy, wbiome); // water color
								waterPixels[regionIndex] = colorMapping.getRGB(wblockData, wbiome); // color of waterlogged block
								waterHeights[regionIndex] = (short) (sectionHeight + cy);
								terrainHeights[regionIndex] = (short) (sectionHeight + cy - 1); // "height" of bottom of water, which will just be 1 block lower so shading works
							} else {
								waterPixels[regionIndex] = colorMapping.getRGB(wblockData, wbiome); // color of block at bottom of water
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
}
