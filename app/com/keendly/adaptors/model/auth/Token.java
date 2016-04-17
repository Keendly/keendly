package com.keendly.adaptors.model.auth;

import org.joda.time.DateTime;

public class Token {

    private String refreshToken;
    private String accessToken;

    private DateTime accessTokenExpiration;
    private boolean refreshed;

    public Token(String accessToken){
        this(null, accessToken, null);
    }

    public Token(String refreshToken, String accessToken){
        this(refreshToken, accessToken, null);
    }

    public Token(String refreshToken, String accessToken, DateTime accessTokenExpiration){
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshed = false;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setRefreshed(){
        this.refreshed = true;
    }

    public boolean gotRefreshed(){
        return refreshed;
    }

    public DateTime getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
}
