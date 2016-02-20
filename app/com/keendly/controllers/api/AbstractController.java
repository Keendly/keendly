package com.keendly.controllers.api;

import com.keendly.adaptors.Adaptor;
import com.keendly.adaptors.AdaptorFactory;
import com.keendly.adaptors.model.Token;
import com.keendly.auth.AuthToken;
import com.keendly.auth.Authenticator;
import com.fasterxml.jackson.databind.JsonNode;
import com.keendly.entities.Provider;
import com.keendly.entities.UserEntity;
import play.libs.Json;
import play.mvc.Controller;

import java.lang.reflect.ParameterizedType;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public abstract class AbstractController<T> extends Controller {

    protected Adaptor getAdaptor(){
        Provider provider = getAuthToken().provider;
        return AdaptorFactory.getInstance(provider, getExternalToken());
    }

    protected Token getExternalToken(){
        AuthToken token = getAuthToken();
        return new Token(token.refreshToken, token.accessToken);
    }

    /**
     * Utility method to be used when creating new entities and we need to set up relation with current user.
     * @return Fake UserEntity object
     */
    protected UserEntity getUserEntity(){
        UserEntity userEntity = new UserEntity();
        AuthToken token = getAuthToken();
        userEntity.id = token.userId;
        return userEntity;
    }

    private AuthToken getAuthToken(){
        return (AuthToken) ctx().args.get("token");
    }

    protected DateTimeFormatter dateTimeFormatter(){
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(lang().toLocale());
    }

    protected T fromRequest(){
        JsonNode json = request().body().asJson();
        return Json.fromJson(json, getGenericClass());
    }

    private Class<T> getGenericClass(){
        return (Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
    }

    protected void refreshTokenIfNeeded(Token externalToken){
        if (externalToken.gotRefreshed()){
            AuthToken token = getAuthToken();
            String newToken = new Authenticator().generate(token.userId, token.provider, externalToken);
            ctx().response().setCookie(KeendlyHeader.SESSION_COOKIE.value, newToken);
        }
    }
}
