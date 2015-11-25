package controllers;

import adaptors.Adaptor;
import adaptors.Adaptors;
import adaptors.model.Tokens;
import entities.Provider;
import entities.User;
import org.apache.commons.lang3.StringUtils;
import play.mvc.Controller;

import static controllers.Constants.SESSION_AUTH;
import static controllers.Constants.SESSION_PROVIDER;
import static play.libs.Json.fromJson;
import static play.libs.Json.parse;

public abstract class AbstractController extends Controller {

    protected String getQueryParam(String key){
        String value[] = request().queryString().get(key);
        if (value != null && value.length > 0){
            return value[0];
        }
        return null;
    }

    public static Adaptor findAdaptor(){
        String dataProvider = session().get(SESSION_PROVIDER);
        if (StringUtils.isNotEmpty(dataProvider)) {
            return Adaptors.getByProvider(Provider.valueOf(dataProvider));
        }
        return null;
    }

    public static Tokens findTokens(){
        String tokensString = session().get(SESSION_AUTH);
        if (tokensString != null){
            Tokens tokens = fromJson(parse(tokensString), Tokens.class);
            return tokens;
        }
        return null;
    }

    public static User getUser(){
        return SessionUtils.getUser(session());
    }
}
