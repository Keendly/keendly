package com.keendly.controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.keendly.adaptors.Adaptor;
import com.keendly.adaptors.AdaptorFactory;
import com.keendly.adaptors.model.auth.Token;
import com.keendly.dao.UserDao;
import com.keendly.entities.UserEntity;
import play.db.jpa.JPA;
import play.libs.Json;
import play.mvc.Controller;

import java.lang.reflect.ParameterizedType;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public abstract class AbstractController<T> extends Controller {

    private static final play.Logger.ALogger LOG = play.Logger.of(AbstractController.class);

    private UserDao userDao = new UserDao();

    protected Adaptor getAdaptor(){
        UserEntity user = null;
        try {
            user = JPA.withTransaction(() -> getUserEntity());
        } catch (Throwable throwable) {
            LOG.error("Error getting user entity", throwable);
        }
        Token token = new Token(user.refreshToken, user.accessToken);
        return AdaptorFactory.getInstance(user.provider, token);
    }

    protected UserEntity getUserEntity(){
        return userDao.findById(getAuthenticatedUserId());
    }

    // to set up relations when creating entities
    protected UserEntity getDummyUserEntity(){
        UserEntity user = new UserEntity();
        user.id = getAuthenticatedUserId();
        return user;
    }

    protected long getAuthenticatedUserId(){
        String userId = ctx().args.get("authenticatedUser").toString();
        return Long.parseLong(userId);
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
}
