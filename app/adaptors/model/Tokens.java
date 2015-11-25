package adaptors.model;

import entities.Provider;

public class Tokens {

    private Provider provider;
    private String refreshToken;
    private String accessToken;

    // needed for json deserializer
    public Tokens(){

    }

    public Tokens(String refreshToken, String accessToken, Provider provider){
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.provider = provider;
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

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }
}
