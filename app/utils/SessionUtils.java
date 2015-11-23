package utils;

import adaptors.Adaptor;
import adaptors.Adaptors;
import adaptors.auth.Tokens;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.Constants;
import models.Provider;
import models.User;
import org.apache.commons.lang3.StringUtils;
import play.libs.Json;
import play.mvc.Http;

import static controllers.Constants.SESSION_AUTH;
import static controllers.Constants.SESSION_PROVIDER;
import static play.libs.Json.fromJson;
import static play.libs.Json.parse;

public class SessionUtils {

    public static Adaptor findAdaptor(Http.Session session){
        String dataProvider = session.get(SESSION_PROVIDER);
        if (StringUtils.isNotEmpty(dataProvider)) {
            return Adaptors.getByProvider(Provider.valueOf(dataProvider));
        }
        return null;
    }

    public static Tokens findTokens(Http.Session session){
        String tokensString = session.get(SESSION_AUTH);
        if (tokensString != null){
            Tokens tokens = fromJson(parse(tokensString), Tokens.class);
            return tokens;
        }
        return null;
    }

    public static User getUser(Http.Session session){
        String user = session.get(Constants.SESSION_USER);
        if (user == null){
            return null;
        }
        JsonNode node = Json.parse(user);
        return Json.fromJson(node, User.class);
    }
}
