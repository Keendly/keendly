package utils;

import java.util.Map;
import java.util.stream.Collectors;

public class WsUtils {

    public static String asFormData(Map<String, String> data){
        return data.entrySet()
                .stream()
                .map(item -> item.getKey() + "=" + item.getValue())
                .collect(Collectors.joining("&"));
    }

}
