package utils;

import play.Play;

public class ConfigUtils {

    public static String parameter(String key){
        return Play.application().configuration().getString(key);
    }
}
