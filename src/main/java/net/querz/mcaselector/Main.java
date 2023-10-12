package net.querz.mcaselector;

import net.querz.mcaselector.cli.CLIJFX;
import net.querz.mcaselector.cli.ParamExecutor;
import net.querz.mcaselector.config.Config;
import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.logging.Logging;
import net.querz.mcaselector.ui.Window;
import net.querz.mcaselector.text.Translation;
import net.querz.mcaselector.validation.ShutdownHooks;
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


	public static int SHADEMOODYNESS = (int)(0.5 * -100);
	public static double ANGLE = degToRad(150); // [90;180]
	public static double HEIGHTANGLE = degToRad(40); // [5;90]
	public static double cosA = round(Math.cos(ANGLE)), sinA = round(Math.sin(ANGLE));
	public static double cotgB = round(1 / Math.tan(HEIGHTANGLE));


	public static double degToRad(double angle){
		return angle / 180 * Math.PI;
	}
	public static double round(double value){ return Math.round(value * 10000.0) / 10000.0;}

	public static void main(String[] args) throws ExecutionException, InterruptedException {

		//RegexMapping.globalMapping = RegexMapping.readMapping(Main.CUSTOM_MAPPING);

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
