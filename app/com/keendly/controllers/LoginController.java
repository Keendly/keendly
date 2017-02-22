package com.keendly.controllers;

import play.mvc.Controller;

public class LoginController extends Controller {

//    private static final Logger.ALogger LOG = Logger.of(LoginController.class);
//
//    private Authenticator authenticator = new Authenticator();
//    private UserDao userDao = new UserDao();
//
//    public Promise<Result> feedlyCallback(String code, String error){
//        if (StringUtils.isNotBlank(error)){
//            Logger.error("Error got in feedly callback: {}", error);
//            return F.Promise.pure(redirect(routes.WebController.login(error)));
//        } else {
//            Credentials credentials = new Credentials();
//            credentials.setAuthorizationCode(code);
//            return loginUser(credentials, Provider.FEEDLY);
//        }
//    }
//
//    public Promise<Result> inoReaderCallback(String code, String state, String error){
//        if (StringUtils.isNotBlank(error)) {
//            Logger.error("Error got in inoreader callback: {}", error);
//            return F.Promise.pure(redirect(routes.WebController.login(error)));
//
//        } else if (!Authenticator.validateStateToken(state, Provider.INOREADER)){
//            Logger.error("Incorrect state received: {}", state);
//            return F.Promise.pure(redirect(routes.WebController.login("Login error, please try again")));
//        } else {
//            Credentials credentials = new Credentials();
//            credentials.setAuthorizationCode(code);
//            return loginUser(credentials, Provider.INOREADER);
//        }
//    }
//
//    public Promise<Result> newsblurCallback(String code, String state, String error){
//        if (StringUtils.isNotBlank(error)) {
//            Logger.error("Error got in newsblur callback: {}", error);
//            return F.Promise.pure(redirect(routes.WebController.login(error)));
//
//        } else if (!Authenticator.validateStateToken(state, Provider.NEWSBLUR)){
//            Logger.error("Incorrect state received: {}", state);
//            return F.Promise.pure(redirect(routes.WebController.login("Login error, please try again")));
//        } else {
//            Credentials credentials = new Credentials();
//            credentials.setAuthorizationCode(code);
//            return loginUser(credentials, Provider.NEWSBLUR);
//        }
//    }
//
//    public Promise<Result> oldReaderLogin(){
//        String email = request().body().asFormUrlEncoded().get("email")[0];
//        String password = request().body().asFormUrlEncoded().get("password")[0];
//        Credentials credentials = new Credentials();
//        credentials.setUsername(email);
//        credentials.setPassword(password);
//        return loginUser(credentials, Provider.OLDREADER).recover(f -> {
//            LOG.error("Error loggin in with theOldReader", f);
//            return redirect(routes.WebController.login("Login error"));
//        });
//    }
//
//    private static int ONE_MONTH = 60 * 60 * 24 * 30;
//    public Promise<Result> loginUser(Credentials credentials, Provider p){
//        Adaptor adaptor = AdaptorFactory.getInstance(p);
//        return adaptor.login(credentials).flatMap(
//                token -> adaptor.getUser().map(user -> {
//                    session("displayName", user.getDisplayName());
//                    List<Long> id = new ArrayList<>();
//                    JPA.withTransaction(() -> {
//                        UserEntity userEntity = findUser(user, p);
//                        userEntity.refreshToken = token.getRefreshToken();
//                        userEntity.accessToken = token.getAccessToken();
//                        userEntity.lastLogin = new Date();
//                        userEntity = JPA.em().merge(userEntity);
//                        id.add(userEntity.id); // why so hacky
//                    });
//                    String t = authenticator.generate(id.get(0), ONE_MONTH);
//                    response().setCookie(KeendlyHeader.SESSION_COOKIE.value, t, ONE_MONTH);
//                    return redirect(routes.WebController.feeds());
//                })
//        );
//    }
//
//    protected UserEntity findUser(ExternalUser externalUser, Provider provider){
//        UserEntity entity = userDao.findByProviderId(externalUser.getId(), provider);
//        if (entity == null){
//            entity = userDao.createUser(externalUser.getId(), provider, externalUser.getUserName());
//        }
//        return entity;
//    }
}
