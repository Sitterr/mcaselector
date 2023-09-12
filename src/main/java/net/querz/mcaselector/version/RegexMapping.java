package net.querz.mcaselector.version;

import net.querz.mcaselector.filter.filters.PaletteFilter;
import net.querz.mcaselector.text.TextHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class RegexMapping {
    private static final Logger LOGGER = LogManager.getLogger(RegexMapping.class);
    static ArrayList<Character> available_chars;
    static ArrayList<Character> addFromTo(char from, char to){
        ArrayList<Character> chars = new ArrayList<>();
        for(char i=from;i<=to;i++){
            chars.add(i);
        }
        return chars;
    }

    static HashMap<String, Character> map = new HashMap<>();
    static HashMap<Character, String> map2 = new HashMap<>();

    public static char mapToChar(String s){
        s = TextHelper.parseBlockName(s);
        return map.get(s);
    }

    public static String mapToString(char c){
        return map2.get(c);
    }

    static {
        available_chars = new ArrayList<>();
        //available_chars.addAll(addFromTo('0', '9'));
        //available_chars.addAll(addFromTo('a', 'z'));
        //available_chars.addAll(addFromTo('A', 'Z'));

        available_chars.addAll(addFromTo((char)161, (char)1500));

        map.put("a", 'a');

        try (BufferedReader bis = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(PaletteFilter.class.getClassLoader().getResourceAsStream("mapping/all_block_names.txt"))))) {
            String line;
            int i = 0;
            while ((line = bis.readLine()) != null) {
                //map.add(line);
                map.put("minecraft:" + line, available_chars.get(i));
                map2.put(available_chars.get(i), "minecraft:" + line);
                i++;
            }
        } catch (IOException ex) {
            LOGGER.error("error reading mapping/all_block_names.txt", ex);
        }
    }

}
