package com.keendly.adaptors.model.auth;

public class Token {

    private String refreshToken;
    private String accessToken;

    private boolean refreshed;

    public Token(String accessToken){
        this(null, accessToken);
    }

    public Token(String refreshToken, String accessToken){
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
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
}
