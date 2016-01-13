package controllers;

import adaptors.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import dao.UserDao;
import entities.Provider;
import entities.UserEntity;
import play.libs.Json;
import play.mvc.Controller;

import java.lang.reflect.ParameterizedType;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public abstract class AbstractController<T> extends Controller {

    protected UserDao userDao = new UserDao();

//
//    public static Adaptor findAdaptor(){
//        String dataProvider = session().get(SESSION_PROVIDER);
//        if (StringUtils.isNotEmpty(dataProvider)) {
//            return Adaptors.getByProvider(Provider.valueOf(dataProvider));
//        }
//        return null;
//    }
//
//    public static Tokens findTokens(){
//        String tokensString = session().get(SESSION_AUTH);
//        if (tokensString != null){
//            Tokens tokens = fromJson(parse(tokensString), Tokens.class);
//            return tokens;
//        }
//        return null;
//    }
//
    protected User getUser(){
        return (User) ctx().args.get("user");
    }

    // TODO this one should somehow retrieve user id from token to avoid calling DB here. Need to have smarter way for creating tokens (private key)
    protected UserEntity getUserEntity(){
        String providerStr = ctx().request().getHeader(KeendlyHeader.PROVIDER.value);
        Provider provider = Provider.valueOf(providerStr);
        User user = getUser();
        UserEntity entity = userDao.findByProviderId(user.getId(), provider);
        if (entity == null){
            entity = userDao.createUser(user.getId(),provider, user.getUserName());
        }
        return entity;
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
