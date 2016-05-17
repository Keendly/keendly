package com.keendly.controllers.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.keendly.auth.Authenticator;
import com.keendly.dao.ClientDao;
import com.keendly.dao.UserDao;
import com.keendly.entities.ClientEntity;
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

public class AuthController extends Controller {

    private static final Logger.ALogger LOG = Logger.of(AuthController.class);

    private UserDao userDao = new UserDao();
    private ClientDao clientDao = new ClientDao();
    private Authenticator authenticator = new Authenticator();

    private static int ONE_HOUR = 60 * 60;
    private static int ONE_MONTH = ONE_HOUR * 24 * 30;
    private enum GrantType {
        BEARER("bearer", ONE_HOUR), // to get token on behalf of a user
        PASSWORD("password", ONE_MONTH), // authenticate with password
        AUTHENTICATION_CODE("authentication_code", ONE_MONTH), // authenticate with code - OAuth
        CLIENT_CREDENTIALS("client_credentials", ONE_HOUR); // for clients, to get token without user's scope

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
        String clientId = json.get("client_id").asText();
        ClientEntity client = clientDao.findByClientId(clientId);
        if (client == null){
            return unauthorized();
        }
        switch (grantType){
            case BEARER:
                String token = json.get("token").asText();
                Integer userId = decode(token, client.clientSecret);
                if (userId == null){
                    return badRequest();
                }
                UserEntity user = userDao.findById(Long.valueOf(userId));
                if (user == null){
                    LOG.error("User with id {} not found", userId);
                    return badRequest();
                }
                String authToken = authenticator.generate(Long.valueOf(userId), grantType.expiresIn);
                return ok(Json.toJson(asAuthToken(authToken, grantType.expiresIn, false)));
            case CLIENT_CREDENTIALS:
                String clientSecret = json.get("client_secret").asText();
                if (client.clientSecret.equals(clientSecret)){
                    String generated = authenticator.generate(-1, grantType.expiresIn);
                    return ok(Json.toJson(asAuthToken(generated, grantType.expiresIn, true)));
                } else {
                    return unauthorized();
                }
            default:
                LOG.error("Grant type not supported {}", grantType.text);
                return badRequest();
        }
    }

    private Integer decode(String token, String secret){
        try {
            Claims claims = Jwts.parser().setSigningKey(secret.getBytes()).parseClaimsJws(token).getBody();
            return claims.get("userId", Integer.class);
        } catch (Exception e){
            LOG.error("Error decoding token", e);
            return null;
        }
    }

    private Map<String, String> asAuthToken(String authToken, int expiresIn, boolean readOnly){
        Map<String, String> ret = new HashMap<>();
        ret.put("accessToken", authToken);
        ret.put("tokeType", "Bearer");
        ret.put("expiresIn", Integer.toString(expiresIn));
        if (readOnly){
            ret.put("scope", "read");
        } else {
            ret.put("scope", "write");
        }
        return ret;
    }
}
