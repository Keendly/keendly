package controllers;

import adaptors.Adaptor;
import adaptors.auth.Tokens;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.request.DeliveryRequest;
import controllers.request.FeedDeliveryRequest;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import utils.SessionUtils;
import views.html.home;

import java.util.List;
import java.util.stream.Collectors;

@With(SecuredAction.class)
public class SecuredController extends Controller {

    public Promise<Result> home(){
        Tokens tokens = SessionUtils.findTokens(session());
        Adaptor adaptor = SessionUtils.findAdaptor(session());
        return adaptor.getSubscriptions(tokens).map(subscriptions ->

            ok(home.render(subscriptions, session()))
        );
    }

    public Promise<Result> deliver(){
        JsonNode json = request().body().asJson();
        DeliveryRequest request = Json.fromJson(json, DeliveryRequest.class);
        Tokens tokens = SessionUtils.findTokens(session());
        Adaptor adaptor = SessionUtils.findAdaptor(session());
        return adaptor.getUnread(extractIds(request.feeds), tokens)
                .map(unread -> {
//                    for(Map.Entry<String, List<Entry>> entries : unread.entrySet()){
//                        if (request.feeds.stream().filter(feed -> feed.id == entries.getKey()).findAny().get().fullArticle){
//                            HtmlFetcher fetcher = new HtmlFetcher();
//                            for (Entry entry : entries.getValue()){
//                                int timeOutMillis = 10 * 1000;
//                                JResult result = fetcher.fetchAndExtract(entry.getUrl(), timeOutMillis, true);
//                                if (result.getText() != null){
//                                    entry.setContent(result.getText());
//                                }
//                            }
//                        }
//                    }


                    return ok("lala");
                });
    }

    private List<String> extractIds(List<FeedDeliveryRequest> feeds){
        return feeds.stream().map(feed -> feed.id).collect(Collectors.toList());
    }
}
