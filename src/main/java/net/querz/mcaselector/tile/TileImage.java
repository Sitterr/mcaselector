package net.querz.mcaselector.tile;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.util.Pair;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.io.FileHelper;
import net.querz.mcaselector.io.mca.Chunk;
import net.querz.mcaselector.io.mca.RegionMCAFile;
import net.querz.mcaselector.math.MathUtil;
import net.querz.mcaselector.point.Point2f;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.realshading.Shade;
import net.querz.mcaselector.realshading.ShadeConstants;
import net.querz.mcaselector.regex.RegexConfig;
import net.querz.mcaselector.selection.ChunkSet;
import net.querz.mcaselector.selection.Selection;
import net.querz.mcaselector.ui.Color;
import net.querz.mcaselector.io.ImageHelper;
import net.querz.mcaselector.version.VersionController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

public final class TileImage {

	private static final Logger LOGGER = LogManager.getLogger(TileImage.class);

	private static final int[] corruptedChunkOverlay = new int[256];

	static {
		Image corrupted = FileHelper.getIconFromResources("img/corrupted");
		PixelReader pr = corrupted.getPixelReader();
		pr.getPixels(0, 0, 16, 16, PixelFormat.getIntArgbPreInstance(), corruptedChunkOverlay, 0, 16);
	}

	private TileImage() {}

	public static void draw(GraphicsContext ctx, Tile tile, float scale, Point2f offset, Selection selection, boolean overlay, boolean showNonexistentRegions) {
		if (tile == null || tile.image == null) {
			if (showNonexistentRegions) {
				ctx.drawImage(ImageHelper.getEmptyTileImage(), offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
			}
		}

		if (tile != null) {
			if (tile.image != null) {
				ctx.setImageSmoothing(ConfigProvider.WORLD.getSmoothRendering());
				ctx.drawImage(tile.image, offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
				ctx.setImageSmoothing(false);
			}

			if (overlay && tile.overlay != null) {
				ctx.setGlobalAlpha(0.5);
				ctx.setImageSmoothing(ConfigProvider.WORLD.getSmoothOverlays());
				ctx.drawImage(tile.getOverlay(), offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
				ctx.setGlobalAlpha(1);
				ctx.setImageSmoothing(false);
			}

			if (selection.isRegionSelected(tile.getLongLocation())) {
				ctx.setFill(ConfigProvider.GLOBAL.getRegionSelectionColor().makeJavaFXColor());
				ctx.fillRect(offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
			} else if (selection.isAnyChunkInRegionSelected(tile.getLongLocation())) {
				if (tile.markedChunksImage == null) {
					createMarkedChunksImage(tile, selection.getSelectedChunks(tile.getLocation()));
				}
				ctx.drawImage(tile.markedChunksImage, offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
			}

		} else if (selection.isInverted()) {
			ctx.setFill(ConfigProvider.GLOBAL.getRegionSelectionColor().makeJavaFXColor());
			ctx.fillRect(offset.getX(), offset.getY(), Tile.SIZE / scale, Tile.SIZE / scale);
		}
	}

	static void createMarkedChunksImage(Tile tile, ChunkSet selection) {
		if (selection == null) {
			tile.markedChunksImage = null;
			return;
		}
		WritableImage wImage = new WritableImage(32, 32);
		PixelWriter writer = wImage.getPixelWriter();

		javafx.scene.paint.Color chunkSelectionColor = ConfigProvider.GLOBAL.getChunkSelectionColor().makeJavaFXColor();

		selection.forEach(s -> {
			Point2i regionChunk = new Point2i(s);
			writer.setColor(regionChunk.getX(), regionChunk.getZ(), chunkSelectionColor);
		});

		tile.markedChunksImage = wImage;
	}

	private static ConcurrentHashMap<Long, Long> locks = new ConcurrentHashMap<Long, Long>();
	private static Object getCacheSyncObject(final Long id) {
		locks.putIfAbsent(id, id);
		return locks.get(id);
	}

	public static Image markImage(Image image, int scale){
		int s = Tile.SIZE / scale;
		int pixels = Tile.PIXELS / (scale * scale);

		var color = javafx.scene.paint.Color.BLUE;
		javafx.scene.image.WritableImage writableImage = new javafx.scene.image.WritableImage(s, s);
		PixelWriter pixelWriter = writableImage.getPixelWriter();
		//int[] pixelBuffer = new int[pixels];

		PixelReader pixelReader = null;
		if(image != null) {
			pixelReader = image.getPixelReader();
		}

		for (int x = 0; x < s; x++) {
			for (int y = 0; y < s; y++) {
				if(pixelReader != null) pixelWriter.setColor(x, y, pixelReader.getColor(x, y));
				else pixelWriter.setColor(x, y, javafx.scene.paint.Color.color(0, 0, 0 ,0));
			}
		}


		// Draw green pixels on the top edge
		for (int x = 0; x < s; x++) {
			pixelWriter.setColor(x, 0, color);
		}

		// Draw green pixels on the bottom edge
		for (int x = 0; x < s; x++) {
			pixelWriter.setColor(x, s - 1, color);
		}

		// Draw green pixels on the left edge
		for (int y = 0; y < s; y++) {
			pixelWriter.setColor(0, y, color);
		}

		// Draw green pixels on the right edge
		for (int y = 0; y < s; y++) {
			pixelWriter.setColor(s - 1, y, color);
		}

		//pixelWriter.setPixels(0, 0, s, s, PixelFormat.getIntArgbPreInstance(), pixelBuffer, 0, size);
		return writableImage;
	}

	public static Image generateImage(RegionMCAFile mcaFile, int scale) {
		Point2i loc = mcaFile.getLocation();
		//synchronized (getCacheSyncObject(loc.asLong())) {

			int size = Tile.SIZE / scale;
			int chunkSize = Tile.CHUNK_SIZE / scale;
			int pixels = Tile.PIXELS / (scale * scale);


			try {

				WritableImage finalImage = new WritableImage(size, size);
				PixelWriter writer = finalImage.getPixelWriter();
				int[] pixelBuffer = new int[pixels];
				int[] waterPixels = ConfigProvider.WORLD.getShade() && ConfigProvider.WORLD.getShadeWater() && !ConfigProvider.WORLD.getRenderCaves() ? new int[pixels] : null;
				short[] terrainHeights = new short[pixels];
				short[] waterHeights = ConfigProvider.WORLD.getShade() && ConfigProvider.WORLD.getShadeWater() && !ConfigProvider.WORLD.getRenderCaves() ? new short[pixels] : null;
				boolean[] shades = null;
				byte[] continuities = null;

				// get shading
				if (ConfigProvider.WORLD.getRenderingMode() == RenderingMode.SHADE) {
					continuities = new byte[ShadeConstants.GLOBAL.rX * ShadeConstants.GLOBAL.rZ];
					shades = new boolean[(ShadeConstants.GLOBAL.rX * 512) * (ShadeConstants.GLOBAL.rZ * 512)];

					for (byte _iz = 0; _iz < ShadeConstants.GLOBAL.rZ; _iz++) {
						for (byte _ix = 0; _ix < ShadeConstants.GLOBAL.rX; _ix++) {
							continuities[_iz * ShadeConstants.GLOBAL.rX + _ix] = Shade.get(loc, shades, _ix, _iz);
						}
					}
				}

				// draw
				for (int _cz = 0; _cz < Tile.SIZE_IN_CHUNKS; _cz++) {
					int cz = ShadeConstants.GLOBAL.flowZ(_cz, 0, Tile.SIZE_IN_CHUNKS);
					for (int _cx = 0; _cx < Tile.SIZE_IN_CHUNKS; _cx++) {
						int cx = ShadeConstants.GLOBAL.flowX(_cx, 0, Tile.SIZE_IN_CHUNKS);
						int index = cz * Tile.SIZE_IN_CHUNKS + cx;

						Chunk data = mcaFile.getChunk(index);

						if (data == null) {
							continue;
						}

						drawChunkImage(data, cx * chunkSize, cz * chunkSize, scale, pixelBuffer, waterPixels, terrainHeights, waterHeights, shades, ShadeConstants.GLOBAL.nflowX(0, 0, ShadeConstants.GLOBAL.rX) * 512, ShadeConstants.GLOBAL.nflowZ(0, 0, ShadeConstants.GLOBAL.rZ) * 512);
					}
				}

				// save shading
				if (ConfigProvider.WORLD.getRenderingMode() == RenderingMode.SHADE) {
					for (byte _iz = 0; _iz < ShadeConstants.GLOBAL.rZ; _iz++) {
						for (byte _ix = 0; _ix < ShadeConstants.GLOBAL.rX; _ix++) {
							Shade.add(loc, shades, continuities[_iz * ShadeConstants.GLOBAL.rX + _ix], _ix, _iz);
						}
					}
				}

				if (ConfigProvider.WORLD.getRenderCaves()) {
					flatShade(pixelBuffer, terrainHeights, scale);
				} else if (ConfigProvider.WORLD.getShade() && !ConfigProvider.WORLD.getRenderLayerOnly()) {
					shade(pixelBuffer, waterPixels, terrainHeights, waterHeights, scale);
				}

				writer.setPixels(0, 0, size, size, PixelFormat.getIntArgbPreInstance(), pixelBuffer, 0, size);

				return finalImage;
			} catch (Exception ex) {
				LOGGER.warn("failed to create image for MCAFile {}", mcaFile.getFile().getName(), ex);
			}
			return null;

		//
		//
		//
		// }
	}

	public enum RenderingMode {
		STANDARD, LAYER, BIOMES, SHADE,
		REGEX_1, REGEX_2, REGEX_3, REGEX_4,
	}
	private static void drawChunkImage(Chunk chunkData, int x, int z, int scale, int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, boolean[] shades, int x0, int z0) {

		if (chunkData.getData() == null) {
			return;
		}
		int dataVersion = chunkData.getData().getIntOrDefault("DataVersion", 0);
		try {

			switch (ConfigProvider.WORLD.getRenderingMode()) {
				case STANDARD -> VersionController.getChunkRenderer(dataVersion).drawChunk(
						chunkData.getData(),
						VersionController.getColorMapping(dataVersion),
						x, z, scale,
						pixelBuffer,
						waterPixels,
						terrainHeights,
						waterHeights,
						ConfigProvider.WORLD.getShade() && ConfigProvider.WORLD.getShadeWater(),
						ConfigProvider.WORLD.getTintBiomes(),
						ConfigProvider.WORLD.getRenderHeight()
				);
				case LAYER -> VersionController.getChunkRenderer(dataVersion).drawLayer(
						chunkData.getData(),
						VersionController.getColorMapping(dataVersion),
						x, z, scale,
						pixelBuffer,
						ConfigProvider.WORLD.getRenderHeight()
				);
				case SHADE -> VersionController.getChunkRenderer(dataVersion).drawShade2(
						chunkData.getData(),
						VersionController.getColorMapping(dataVersion),
						x, z, scale,
						pixelBuffer,
						waterPixels,
						terrainHeights,
						waterHeights,
						shades,
						x0, z0,
						ConfigProvider.WORLD.getShade() && ConfigProvider.WORLD.getShadeWater(),
						ConfigProvider.WORLD.getTintBiomes(),
						ConfigProvider.WORLD.getRenderHeight()
				);
				case BIOMES -> VersionController.getChunkRenderer(dataVersion).drawBiomes(
						chunkData.getData(),
						VersionController.getColorMapping(dataVersion),
						x, z, scale,
						pixelBuffer,
						ConfigProvider.WORLD.getRenderHeight()
				);
				case REGEX_1, REGEX_2, REGEX_3, REGEX_4 -> VersionController.getChunkRenderer(dataVersion).drawRegex(
						chunkData.getData(),
						VersionController.getColorMapping(dataVersion),
						RegexConfig.getMapping(),
						RegexConfig.getPattern(),
						RegexConfig.getDisplayGroup(),
						x, z, scale,
						pixelBuffer,
						waterPixels,
						terrainHeights,
						waterHeights,
						ConfigProvider.WORLD.getShade() && ConfigProvider.WORLD.getShadeWater(),
						ConfigProvider.WORLD.getTintBiomes(),
						ConfigProvider.WORLD.getRenderHeight()
				);
			}

		} catch (Exception ex) {
			LOGGER.warn("failed to draw chunk {}", chunkData.getAbsoluteLocation(), ex);

			// TODO: scale corrupted image
			for (int cx = 0; cx < Tile.CHUNK_SIZE; cx += scale) {
				for (int cz = 0; cz < Tile.CHUNK_SIZE; cz += scale) {
					int srcIndex = cz * Tile.CHUNK_SIZE + cx;
					int dstIndex = (z + cz / scale) * Tile.SIZE / scale + (x + cx / scale);
					pixelBuffer[dstIndex] = corruptedChunkOverlay[srcIndex];
					terrainHeights[dstIndex] = 64;
					waterHeights[dstIndex] = 64;
				}
			}
		}
	}

	private static void flatShade(int[] pixelBuffer, short[] terrainHeights, int scale) {
		int size = Tile.SIZE / scale;
		int index = 0;
		for (int z = 0; z < size; z++) {
			for (int x = 0; x < size; x++, index++) {
				int altitudeShade = MathUtil.clamp(16 * terrainHeights[index] / 64, -50, 50);
				pixelBuffer[index] = Color.shade(pixelBuffer[index], altitudeShade * 4);
			}
		}
	}

	private static void shade(int[] pixelBuffer, int[] waterPixels, short[] terrainHeights, short[] waterHeights, int scale) {
		if (!ConfigProvider.WORLD.getShadeWater() || !ConfigProvider.WORLD.getShade()) {
			waterHeights = terrainHeights;
		}

		int size = Tile.SIZE / scale;

		int index = 0;
		for (int z = 0; z < size; z++) {
			for (int x = 0; x < size; x++, index++) {
				float xShade, zShade;

				if (pixelBuffer[index] == 0) {
					continue;
				}

				if (terrainHeights[index] != waterHeights[index]) {
					float ratio = 0.5f - 0.5f / 40f * (float) ((waterHeights[index]) - (terrainHeights[index]));
					pixelBuffer[index] = Color.blend(pixelBuffer[index], waterPixels[index], ratio);
				} else {
					if (z == 0) {
						zShade = (waterHeights[index + size]) - (waterHeights[index]);
					} else if (z == size - 1) {
						zShade = (waterHeights[index]) - (waterHeights[index - size]);
					} else {
						zShade = ((waterHeights[index + size]) - (waterHeights[index - size])) * 2;
					}

					if (x == 0) {
						xShade = (waterHeights[index + 1]) - (waterHeights[index]);
					} else if (x == size - 1) {
						xShade = (waterHeights[index]) - (waterHeights[index - 1]);
					} else {
						xShade = ((waterHeights[index + 1]) - (waterHeights[index - 1])) * 2;
					}

					double shade = -(ShadeConstants.GLOBAL.cosA * xShade + -ShadeConstants.GLOBAL.sinA * zShade);
					if (shade < -8) {
						shade = -8;
					}
					if (shade > 8) {
						shade = 8;
					}

					int altitudeShade = 16 * (waterHeights[index] - 64) / 255;
					if (altitudeShade < -4) {
						altitudeShade = -4;
					}
					if (altitudeShade > 24) {
						altitudeShade = 24;
					}

					shade += altitudeShade;

					if(ConfigProvider.WORLD.getRenderingMode() == RenderingMode.SHADE){
						//if(shade > 0)
							pixelBuffer[index] = Color.shade(pixelBuffer[index], (int) (shade * 3));
					}else{
						pixelBuffer[index] = Color.shade(pixelBuffer[index], (int) (shade * 8));
					}

				}
			}
		}
	}
}
