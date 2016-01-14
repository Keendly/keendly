package auth;

public class Token {

    private String userId;
    private String provider;
    private String providerId;
    private String refreshToken;
    private String accessToken;

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

}
