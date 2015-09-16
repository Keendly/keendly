package adaptors.feedly;

import adaptors.auth.Tokens;
import adaptors.auth.User;
import adaptors.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.WS;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import utils.ConfigUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        WS.class,
        ConfigUtils.class
})
public class FeedlyAdaptorTest {

    private static final String FEEDLY_URL = "test";

    private FeedlyAdaptor feedlyAdaptor;

    @Before
    public void setUp(){
        PowerMockito.mockStatic(WS.class);
        PowerMockito.mockStatic(ConfigUtils.class);
        PowerMockito.when(ConfigUtils.parameter(FeedlyAdaptor.FeedlyParam.URL.value)).thenReturn(FEEDLY_URL);
        feedlyAdaptor = new FeedlyAdaptor();
    }

    @Test
    public void getTokenByCode(){
        // given
        String json =
                "{" +
                        "\"access_token\":\"access\"," +
                        "\"plan\":\"standard\"," +
                        "\"refresh_token\":\"refresh\"," +
                        "\"id\":\"445013c4-b8d3-4128-8fc4-bd291f1abf11\"," +
                        "\"expires_in\":604800," +
                        "\"token_type\":\"Bearer\"," +
                        "\"provider\":\"GooglePlus\"" +
                "}";
        mockPOST("/auth/token", HttpStatus.SC_OK, json);

        // when
        Promise<Tokens> tokensPromise = feedlyAdaptor.getTokens("doesnt_matter");
        Tokens tokens = tokensPromise.get(1);

        // then
        assertNotNull(tokens);
        assertEquals("refresh", tokens.getRefreshToken());
        assertEquals("access", tokens.getAccessToken());
    }

    @Test(expected = ApiException.class)
    public void getTokenByCode_notOkFeedlyResponse(){
        // given
        mockPOST("/auth/token", HttpStatus.SC_INTERNAL_SERVER_ERROR, StringUtils.EMPTY);

        // when
        Promise<Tokens> tokensPromise = feedlyAdaptor.getTokens("doesnt_matter");
        tokensPromise.get(1);
    }

    @Test
    public void getUser(){
        // given
        String json =
                "{" +
                        "\"id\":\"12345\"," +
                        "\"email\":\"test@keendly.com\"" +
                "}";
        mockGET("/profile", HttpStatus.SC_OK, json);

        // when
        Promise<User> userPromise = feedlyAdaptor.getUser(new Tokens());
        User user = userPromise.get(1);

        // then
        assertNotNull(user);
        assertEquals("12345", user.getId());
        assertEquals("test@keendly.com", user.getUserName());
    }

    @Test(expected = ApiException.class)
    public void getUser_notOkFeedlyResponse(){
        // given
        mockGET("/profile", HttpStatus.SC_INTERNAL_SERVER_ERROR, StringUtils.EMPTY);

        // when
        Promise<User> userPromise = feedlyAdaptor.getUser(new Tokens());
        userPromise.get(1000);
    }

    @Test
    public void getUser_accessTokenExpired(){
        // given
        mockGET("/profile", HttpStatus.SC_UNAUTHORIZED, StringUtils.EMPTY);

        String refreshTokenResponse =
                "{" +
                        "\"access_token\":\"newAccessToken\"" +
                "}";
        mockPOST("/auth/token", HttpStatus.SC_OK, refreshTokenResponse);
        Tokens tokens = new Tokens();

        // when
        Promise<User> userPromise = feedlyAdaptor.getUser(tokens);
        userPromise.get(1000);

        // then
        assertEquals("newAccessToken", tokens.getAccessToken());
        PowerMockito.verifyStatic();
        WS.url(FEEDLY_URL + "/auth/token"); // refresh token called
    }

    @Test(expected = ApiException.class)
    public void getUser_refreshTokenExpired(){
        // given
        mockGET("/profile", HttpStatus.SC_UNAUTHORIZED, StringUtils.EMPTY);
        mockPOST("/auth/token", HttpStatus.SC_UNAUTHORIZED, StringUtils.EMPTY);

        // when
        Promise<User> userPromise = feedlyAdaptor.getUser(new Tokens());
        userPromise.get(1000);
    }

    private void mockPOST(String url, int status, String content){
        WSRequest request = request();
        WSResponse response = response(status, content);
        when(request.post(any(JsonNode.class))).thenReturn(Promise.pure(response));
        PowerMockito.when(WS.url(FEEDLY_URL + url)).thenReturn(request);
    }

    private void mockGET(String url, int status, String content){
        WSRequest request = request();
        WSResponse response = response(status, content);
        when(request.get()).thenReturn(Promise.pure(response));
        PowerMockito.when(WS.url(FEEDLY_URL + url)).thenReturn(request);
    }

    private WSRequest request(){
        WSRequest request = mock(WSRequest.class);
        when(request.setHeader(anyString(), anyString())).thenReturn(request);
        return request;
    }

    private WSResponse response(int status, String content){
        WSResponse response = mock(WSResponse.class);
        when(response.getStatus()).thenReturn(status);
        if (StringUtils.isNotEmpty(content)){
            when(response.asJson()).thenReturn(Json.parse(content));
        }
        return response;
    }

}
