package net.querz.mcaselector.version;

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
    static ArrayList<Character> available_chars;
    static HashMap<String, Character> map1 = new HashMap<>();
    static HashMap<Character, String> map2 = new HashMap<>();
    static HashMap<Character, Integer> map3 = new HashMap<>();

    public static char encode(String s){
        s = TextHelper.parseBlockName(s);

        return map1.get(s);
    }

    public static String decode(char c){
        return map2.get(c);
    }

    public static String code(String s){
        char encode = encode(s);
        String decode = decode(encode);
        return decode == null ? s : decode;
    }

    public static int colorcode(char c){
        //s = TextHelper.parseBlockName(s);
        if(!map3.containsKey(c)) return Integer.MIN_VALUE;
        return map3.get(c);
    }

    public static void readCustomMapping(String customMapping){
        if(!customMapping.endsWith(";")) customMapping = customMapping + ";";

        final Pattern TYPE1 = Pattern.compile("^([^#]*?)=([\\w\\d])$"); // mapping name to char
        final Pattern TYPE2 = Pattern.compile("^([\\w\\d])=([^#]*)"); // mapping char to name
        final Pattern TYPE3 = Pattern.compile("^(.*?)=#([a-fA-F0-9]{6})"); // mapping name to color

        String[] mappings = customMapping.split(";");
        for (String mapping : mappings) {
            Matcher m1 = TYPE1.matcher(mapping);
            Matcher m2 = TYPE2.matcher(mapping);
            Matcher m3 = TYPE3.matcher(mapping);

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

                map3.put(ch, TextHelper.parseInt(hexColor, 16));
            }
        }
    }


    static {
        available_chars = new ArrayList<>();
        //available_chars.addAll(addFromTo('0', '9'));
        //available_chars.addAll(addFromTo('a', 'z'));
        //available_chars.addAll(addFromTo('A', 'Z'));

        available_chars.addAll(addFromTo((char)161, (char)1500));

        map1.put("a", 'a');

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
    }
    static ArrayList<Character> addFromTo(char from, char to){
        ArrayList<Character> chars = new ArrayList<>();
        for(char i=from;i<=to;i++){
            chars.add(i);
        }
        return chars;
    }

}
