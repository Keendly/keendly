package auth;

import entities.Provider;

public class AuthToken {

    public long userId;
    public Provider provider;
    public String accessToken;
    public String refreshToken;
}
