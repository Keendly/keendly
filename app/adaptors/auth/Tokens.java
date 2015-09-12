package adaptors.auth;

public class Tokens {

    private String refreshToken;
    private String accessToken;

    public Tokens(String refreshToken, String accessToken){
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }
}
