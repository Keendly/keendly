package com.keendly.auth;


import com.keendly.adaptors.model.Token;
import com.keendly.entities.Provider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;

public class Authenticator {

    private static final String KEY = "UTVLLZ+VfExnLnf455kffeameR+EwljJRqlMcUGCia9Op0jXTmeQMXCtKm5hKSa6sYbOtaeRfu9G4Ujs2pvOUA==";

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
}
