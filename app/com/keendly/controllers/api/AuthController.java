package com.keendly.controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.keendly.auth.Authenticator;
import com.keendly.dao.UserDao;
import com.keendly.entities.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import play.Logger;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.HashMap;
import java.util.Map;

import static com.keendly.utils.ConfigUtils.parameter;

public class AuthController extends Controller {

    private static final Logger.ALogger LOG = Logger.of(AuthController.class);

    private static final String BEARER_KEY = parameter("auth.bearer_token.key");

    private UserDao userDao = new UserDao();
    private Authenticator authenticator = new Authenticator();

    private static int ONE_HOUR = 60 * 60;
    private static int ONE_MONTH = ONE_HOUR * 24 * 30;
    private enum GrantType {
        BEARER("bearer", ONE_HOUR), // to get token on behalf of user
        PASSWORD("password", ONE_MONTH), // authenticate with password
        AUTHENTICATION_CODE("authentication_code", ONE_MONTH); // authenticate with code - OAuth

        String text;
        int expiresIn;
        GrantType(String s, int expiresIn){
            this.expiresIn = expiresIn;
            this.text = s;
        }

        static GrantType fromString(String s){
            for (GrantType grantType : GrantType.values()){
                if (grantType.text.equals(s)){
                    return grantType;
                }
            }
            return null;
        }
    }

    @Transactional
    public Result authenticate() throws Exception{
        JsonNode json = request().body().asJson();
        String grantTypeString = json.get("grant_type").asText();
        GrantType grantType = GrantType.fromString(grantTypeString);
        if (grantType == null){
            LOG.error("Wrong grant_type {}", grantTypeString);
            return badRequest();
        }
        switch (grantType){
            case BEARER:
                String token = json.get("token").asText();
                Integer userId = decode(token);
                if (userId == null){
                    return badRequest();
                }
                UserEntity user = userDao.findById(Long.valueOf(userId));
                if (user == null){
                    LOG.error("User with id {} not found", userId);
                    return badRequest();
                }
                String authToken = authenticator.generate(Long.valueOf(userId), grantType.expiresIn);
                return ok(Json.toJson(asAuthToken(authToken, grantType.expiresIn)));
            default:
                LOG.error("Grant type not supported {}", grantType.text);
                return badRequest();
        }
    }

    private Integer decode(String token){
        try {
            Claims claims = Jwts.parser().setSigningKey(BEARER_KEY.getBytes()).parseClaimsJws(token).getBody();
            return claims.get("userId", Integer.class);
        } catch (Exception e){
            LOG.error("Error decoding token", e);
            return null;
        }
    }

    private Map<String, String> asAuthToken(String authToken, int expiresIn){
        Map<String, String> ret = new HashMap<>();
        ret.put("accessToken", authToken);
        ret.put("tokeType", "Bearer");
        ret.put("expiresIn", Integer.toString(expiresIn));
        ret.put("scope", "write");
        return ret;
    }
}
