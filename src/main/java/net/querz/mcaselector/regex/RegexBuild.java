package net.querz.mcaselector.regex;

import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.tile.TileImage;

public class RegexBuild {

    private String pattern, group, mapping;

    public RegexBuild(String pattern, String group, String mapping){
        this.pattern = pattern;
        this.group = group;
        this.mapping = mapping;
    }
    public String getPattern(){
        return pattern;
    }
    public String getGroup(){
        return group;
    }
    public String getMapping(){
        return mapping;
    }

    public static RegexBuild fromWorldConfig() {
        if(ConfigProvider.WORLD.getRegexPattern().isEmpty()) return LAYER;
        return new RegexBuild(ConfigProvider.WORLD.getRegexPattern(), ConfigProvider.WORLD.getRegexDisplayGroup(), ConfigProvider.WORLD.getRegexMapping());
    }

    public static final RegexBuild EMPTY = new RegexBuild("", "", "");
    public static final RegexBuild DEFAULT = new RegexBuild("^a*(?<CAPTURE>.)", "CAPTURE", "air=a;cave_air=a;barrier=a;structure_void=a;light=a;");
    public static final RegexBuild LAYER = new RegexBuild("^(?<CAPTURE>.)", "CAPTURE", "");
    public static final RegexBuild DEEP_WATER = new RegexBuild("^a*w{0,20}(?<CAPTURE>.)", "CAPTURE", "water=w;air=a;cave_air=a;barrier=a;structure_void=a;light=a;");
    public static final RegexBuild NO_WATER = new RegexBuild("^(a|w)*(?<CAPTURE>.)", "CAPTURE", "water=w;air=a;cave_air=a;barrier=a;structure_void=a;light=a;");
    public static final RegexBuild NO_FLORA = new RegexBuild("", "", "");
    public static final RegexBuild CAVES = new RegexBuild("^a*[^a]+a+(?<CAPTURE>[^a])", "CAPTURE", "air=a;cave_air=a;barrier=a;structure_void=a;light=a;");
    public static final RegexBuild SOMETHING1 = new RegexBuild("dd(?<CAPTURE>d)", "CAPTURE", "diamond_ore=d;deepslate_diamond_ore=d;");
    public static final RegexBuild SOMETHING2 = new RegexBuild("", "", "");

    //public static final BuildInRegex DEFAULT = new BuildInRegex("", "", "");


}
