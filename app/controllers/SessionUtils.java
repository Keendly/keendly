package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import entities.User;
import play.libs.Json;
import play.mvc.Http;

public class SessionUtils {

    public static User getUser(Http.Session session){
        String user = session.get(Constants.SESSION_USER);
        if (user == null){
            return null;
        }
        JsonNode node = Json.parse(user);
        return Json.fromJson(node, User.class);
    }
}
