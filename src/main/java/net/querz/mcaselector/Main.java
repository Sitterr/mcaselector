package net.querz.mcaselector;

import net.querz.mcaselector.cli.CLIJFX;
import net.querz.mcaselector.cli.ParamExecutor;
import net.querz.mcaselector.config.Config;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.logging.Logging;
import net.querz.mcaselector.ui.Window;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.validation.ShutdownHooks;
import net.querz.mcaselector.version.RegexMapping;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.swing.*;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class Main {

	public static AtomicInteger timeLoop = new AtomicInteger(), timeLogic = new AtomicInteger();


	public static final String DEFAULT = "^a*(?<CAPTURE>.?)";
	public static final String LAYER_ONLY = "^(?<CAPTURE>.?)";
	public static final String WATER_ONLY = "(?<CAPTURE>(w|1))";
	public static final String DEEP_WATER = "^[a-e]*w{0,20}(?<CAPTURE>.?)";
	public static final String CAVES_ORIGINAL = "^[a-e]*[^a-e][a-e]+(?<CAPTURE>.?)";

	public static final String EXPERIMENTAL = "(?<CAPTURE>l)";

	public static final Pattern PATTERN = Pattern.compile(DEFAULT);
	public static final String GROUP = "CAPTURE";
	public static final String CUSTOM_MAPPING = "water=w;w=#e44d3f;air=a;cave_air=a;barrier=a;structure_void=a;light=a;";

	public static void main(String[] args) throws ExecutionException, InterruptedException {

		RegexMapping.readCustomMapping(Main.CUSTOM_MAPPING);

		//System.out.println((int)RegexMapping.mapToChar("air") + " | " + (int)RegexMapping.mapToChar("cave_air"));

		if(true) {
			Logging.setLogDir(Config.BASE_LOG_DIR);
			Logging.updateThreadContext();
			Logger LOGGER = LogManager.getLogger(Main.class);

			LOGGER.debug("java version {}", System.getProperty("java.version"));
			LOGGER.debug("jvm max memory {}", Runtime.getRuntime().maxMemory());

			ParamExecutor ex = new ParamExecutor(args);
			Future<Boolean> future = ex.run();
			if (future != null && future.get()) {
				System.exit(0);
			}

			if (!CLIJFX.hasJavaFX()) {
				JOptionPane.showMessageDialog(null, "Please install JavaFX for your Java version (" + System.getProperty("java.version") + ") to use MCA Selector.", "Missing JavaFX", JOptionPane.ERROR_MESSAGE);
				System.exit(0);
			}

			ConfigProvider.loadGlobalConfig();
			ConfigProvider.loadOverlayConfig();
			ShutdownHooks.addShutdownHook(ConfigProvider::saveAll);
			Translation.load(ConfigProvider.GLOBAL.getLocale());
			Locale.setDefault(ConfigProvider.GLOBAL.getLocale());

			Window.launch(Window.class, args);
		}
	}
}
