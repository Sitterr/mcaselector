package net.querz.mcaselector.regex;

public class BuildInRegex {

    private String pattern, group, mapping;

    public BuildInRegex(String pattern, String group, String mapping){
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


    public static final BuildInRegex DEFAULT = new BuildInRegex("^a*(?<CAPTURE>.?)", "CAPTURE", "air=a;cave_air=a;barrier=a;structure_void=a;light=a;");
    public static final BuildInRegex LAYER = new BuildInRegex("^(?<CAPTURE>.?)", "CAPTURE", "");
    public static final BuildInRegex DEEP_WATER = new BuildInRegex("^a*w{0,20}(?<CAPTURE>.?)", "CAPTURE", "water=w;air=a;cave_air=a;barrier=a;structure_void=a;light=a;");
    public static final BuildInRegex NO_WATER = new BuildInRegex("^(a|w)*(?<CAPTURE>.?)", "CAPTURE", "water=w;air=a;cave_air=a;barrier=a;structure_void=a;light=a;");
    public static final BuildInRegex CAVES = new BuildInRegex("^a*[^a]a+(?<CAPTURE>.?)", "CAPTURE", "air=a;cave_air=a;barrier=a;structure_void=a;light=a;");
    public static final BuildInRegex NO_FLORA = new BuildInRegex("", "", "");
    //public static final BuildInRegex DEFAULT = new BuildInRegex("", "", "");
    //public static final BuildInRegex DEFAULT = new BuildInRegex("", "", "");


}
