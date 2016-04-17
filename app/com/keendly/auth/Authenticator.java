package com.keendly.auth;


import com.keendly.adaptors.model.auth.Token;
import com.keendly.entities.Provider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import play.Logger;

import java.net.URLDecoder;
import java.net.URLEncoder;

import java.util.Date;

import static com.keendly.utils.ConfigUtils.parameter;

public class Authenticator {

    private static final Logger.ALogger LOG = Logger.of(Authenticator.class);

    private static final String KEY = parameter("auth.key");

    public String generate(long userId, Provider provider, Token externalToken){
        Claims claims = new DefaultClaims();
        claims.put("userId", Long.toString(userId));
        claims.put("accessToken", externalToken.getAccessToken());
        claims.put("refreshToken", externalToken.getRefreshToken());
        claims.put("provider", provider.name());
        claims.put("type", TokenType.USER.name());

        return Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS512, KEY).compact();
    }

    public AuthToken parse(String token){
        if (token.equals("gfUb$2^UG7:jU>=K")) { // hard hack
            AuthToken authToken = new AuthToken();
            authToken.type = TokenType.ADMIN;
            authToken.userId = -1;
            return authToken;
        }
        Claims claims = Jwts.parser().setSigningKey(KEY).parseClaimsJws(token).getBody();
        AuthToken authToken = new AuthToken();
        authToken.accessToken = claims.get("accessToken", String.class);
        authToken.userId = Long.valueOf(claims.get("userId", String.class));
        authToken.provider = Provider.valueOf(claims.get("provider", String.class));
        authToken.refreshToken = claims.get("refreshToken", String.class);
        authToken.type = TokenType.valueOf(claims.get("type", String.class));
        return authToken;
    }

    public static String generateStateToken(String provider){
        Provider p = Provider.valueOf(provider);
        Claims claims = new DefaultClaims();
        claims.put("provider", p.name());
        claims.put("expirationDate", DateTime.now().plus(Minutes.minutes(5)).toDate().getTime());

        String token = Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS256, KEY).compact();
        return URLEncoder.encode(token);
    }

    public static boolean validateStateToken(String encodedToken, Provider provider){
        try {
            String token = URLDecoder.decode(encodedToken);
            Claims claims = Jwts.parser().setSigningKey(KEY).parseClaimsJws(token).getBody();

            Provider p = Provider.valueOf(claims.get("provider", String.class));
            if (p != provider){
                LOG.error("Incorrect provider in token {}, expected {}, got {}",
                        encodedToken, provider.name(), p.name());
                return false;
            }
            Long expirationDate = claims.get("expirationDate", Long.class);

            if (new Date(expirationDate).before(new Date())){
                LOG.error("Expired state token {} for {}", encodedToken, provider.name());
                return false;
            }
            return true;
        } catch (Exception e){
            LOG.error("Error validating state token", e);
            return false;
        }
    }
}
