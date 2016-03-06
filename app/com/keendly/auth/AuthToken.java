package com.keendly.auth;

import com.keendly.entities.Provider;

public class AuthToken {

    public long userId;
    public Provider provider;
    public String accessToken;
    public String refreshToken;
    public TokenType type;
}
