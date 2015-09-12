package utils;

import play.Play;

public class PlayUtils {

    public static String configParam(String key){
        return Play.application().configuration().getString(key);
    }
}
