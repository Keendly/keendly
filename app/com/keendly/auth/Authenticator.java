package com.keendly.auth;


import com.keendly.entities.Provider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import org.joda.time.DateTime;
import play.Logger;

import java.net.URLDecoder;
import java.net.URLEncoder;

import static com.keendly.utils.ConfigUtils.parameter;

public class Authenticator {

    private static final Logger.ALogger LOG = Logger.of(Authenticator.class);

    private static final String KEY = parameter("auth.key");

    public String generate(long userId, int expiresIn){
        Claims claims = new DefaultClaims();
        claims.put("userId", Long.toString(userId));
        claims.setExpiration(DateTime.now().plusSeconds(expiresIn).toDate());

        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, KEY)
                .compact();
    }

    public String parse(String token){
        Claims claims = Jwts.parser().setSigningKey(KEY).parseClaimsJws(token).getBody();
        return claims.get("userId", String.class);
    }

    // used on login page to generate state strings for oauth
    public static String generateStateToken(String provider){
        Provider p = Provider.valueOf(provider);
        Claims claims = new DefaultClaims();
        claims.put("provider", p.name());
        claims.setExpiration(DateTime.now().plusMinutes(5).toDate());

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

            return true;
        } catch (Exception e){
            LOG.error("Error validating state token", e);
            return false;
        }
    }
}
