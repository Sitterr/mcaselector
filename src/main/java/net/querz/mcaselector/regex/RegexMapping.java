package net.querz.mcaselector.regex;

import net.querz.mcaselector.config.ConfigProvider;
import net.querz.mcaselector.filter.filters.PaletteFilter;
import net.querz.mcaselector.text.TextHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexMapping {
    private static final Logger LOGGER = LogManager.getLogger(RegexMapping.class);

    private HashMap<String, Character> map1;
    private HashMap<Character, String> map2;
    private HashMap<Character, Integer> map3;
    public RegexMapping(HashMap<String, Character> map1, HashMap<Character, String> map2, HashMap<Character, Integer> map3){
        this.map1 = map1;
        this.map2 = map2;
        this.map3 = map3;
    }

    public char encode(String s){
        s = TextHelper.parseBlockName(s);
        return map1.get(s);
    }

    public String decode(char c){
        return map2.get(c);
    }

    public String code(String s){
        char encode = encode(s);
        String decode = decode(encode);
        return decode == null ? s : decode;
    }

    public int colorcode(char c){
        //s = TextHelper.parseBlockName(s);
        if(!map3.containsKey(c)) return Integer.MIN_VALUE;
        return map3.get(c);
    }



    private static RegexMapping coreMapping;
    static {
        coreMapping = setUpCoreMapping();
    }

    public static RegexMapping readMapping(String customMapping){
        customMapping = customMapping.replaceAll("\n", "");
        if(!customMapping.endsWith(";")) customMapping = customMapping + ";";

        final Pattern TYPE1 = Pattern.compile("^([^#]*?)=([\\w\\d])$"); // encode name to char
        final Pattern TYPE2 = Pattern.compile("^([\\w\\d])=([^#]*)"); // decode char to name
        final Pattern TYPE3 = Pattern.compile("^(.*?)=#([a-fA-F0-9]{6})"); // decode char to color

        HashMap<String, Character> map1 = new HashMap<>(coreMapping.map1);
        HashMap<Character, String> map2 = new HashMap<>(coreMapping.map2);
        HashMap<Character, Integer> map3 = new HashMap<>(coreMapping.map3);

        String[] statements = customMapping.split(";");
        for (String statement : statements) {
            Matcher m1 = TYPE1.matcher(statement);
            Matcher m2 = TYPE2.matcher(statement);
            Matcher m3 = TYPE3.matcher(statement);

            if(m1.matches()){
                String blockName = TextHelper.parseBlockName(m1.group(1));
                char ch = m1.group(2).charAt(0);

                map1.put(blockName, ch);
            } else if(m2.matches()){
                String blockName = TextHelper.parseBlockName(m2.group(2));
                char ch = m2.group(1).charAt(0);

                map2.put(ch, blockName);
            } else if(m3.matches()){
                char ch = m3.group(1).charAt(0);
                String hexColor = m3.group(2);

                map3.put(ch, TextHelper.parseInt(hexColor, 16) | 0xFF000000);
            }
        }

        return new RegexMapping(map1, map2, map3);
    }
    private static RegexMapping setUpCoreMapping() {
        ArrayList<Character> available_chars = new ArrayList<>(addFromTo((char) 161, (char) 1500));

        HashMap<String, Character> map1 = new HashMap<>();

        try (BufferedReader bis = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(PaletteFilter.class.getClassLoader().getResourceAsStream("mapping/all_block_names.txt"))))) {
            String line;
            int i = 0;
            while ((line = bis.readLine()) != null) {
                //map.add(line);
                String blockName = TextHelper.parseBlockName(line);
                map1.put(blockName, available_chars.get(i));
                //map2.put(available_chars.get(i), blockName);
                i++;
            }
        } catch (IOException ex) {
            LOGGER.error("error reading mapping/all_block_names.txt", ex);
        }

        return new RegexMapping(map1, new HashMap<>(), new HashMap<>());
    }
    static ArrayList<Character> addFromTo(char from, char to){
        ArrayList<Character> chars = new ArrayList<>();
        for(char i=from;i<=to;i++){
            chars.add(i);
        }
        return chars;
    }

}
