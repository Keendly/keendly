package auth;


import adaptors.model.Token;
import entities.Provider;
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

        return Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS512, KEY).compact();
    }

    public AuthToken parse(String token){
        Claims claims = Jwts.parser().setSigningKey(KEY).parseClaimsJws(token).getBody();
        AuthToken authToken = new AuthToken();
        authToken.accessToken = claims.get("accessToken", String.class);
        authToken.userId = Long.valueOf(claims.get("userId", String.class));
        authToken.provider = Provider.valueOf(claims.get("provider", String.class));
        authToken.refreshToken = claims.get("refreshToken", String.class);
        return authToken;
    }
}