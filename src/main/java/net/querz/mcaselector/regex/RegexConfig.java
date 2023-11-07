package net.querz.mcaselector.regex;

import net.querz.mcaselector.config.ConfigProvider;

import java.util.regex.Pattern;

public class RegexConfig {
    private static Pattern pattern;
    private static String oldPattern = "";
    public static Pattern getPattern(){
        String currentPattern = ConfigProvider.WORLD.getRegexPattern();
        if(!currentPattern.equals(oldPattern)){
            oldPattern = currentPattern;
            pattern = Pattern.compile(currentPattern);
        }
        return pattern;
    }

    public static String getDisplayGroup(){
        return ConfigProvider.WORLD.getRegexDisplayGroup();
    }

    private static RegexMapping mapping;
    private static String oldMapping = null;
    public static RegexMapping getMapping(){
        String currentMapping = ConfigProvider.WORLD.getRegexMapping();
        if(!currentMapping.equals(oldMapping)){
            oldMapping = currentMapping;
            mapping = RegexMapping.readMapping(currentMapping);
        }
        return mapping;
    }
}
