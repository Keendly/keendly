package utils;

import adaptors.Adaptor;
import adaptors.Adaptors;
import models.Provider;
import adaptors.auth.Tokens;
import org.apache.commons.lang3.StringUtils;
import play.mvc.Http;

import static controllers.Constants.SESSION_AUTH;
import static controllers.Constants.SESSION_PROVIDER;
import static play.libs.Json.fromJson;
import static play.libs.Json.parse;

public class SessionUtils {

    public static Adaptor findAdaptor(Http.Session session){
        String dataProvider = session.get(SESSION_PROVIDER);
        if (StringUtils.isNotEmpty(dataProvider)){
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
}
