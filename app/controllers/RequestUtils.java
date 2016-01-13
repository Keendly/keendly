package controllers;

import adaptors.Adaptor;
import adaptors.Adaptors;
import auth.Tokens;
import entities.Provider;
import play.mvc.Http;

public class RequestUtils {

    public static Adaptor getAdaptor(Http.Request request){
        String providerStr = request.getHeader(KeendlyHeader.PROVIDER.value);
        Provider provider = Provider.valueOf(providerStr);
        return Adaptors.getByProvider(provider);
    }

    public static Tokens getTokens(Http.Request request){
        String accessToken = request.getHeader(KeendlyHeader.ACCESS_TOKEN.value);
        String refreshToken = request.getHeader(KeendlyHeader.REFRESH_TOKEN.value);

        return new Tokens(refreshToken, accessToken);
    }


}
