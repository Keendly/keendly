package controllers;

import adaptors.Adaptor;
import adaptors.auth.Entry;
import adaptors.auth.Tokens;
import com.fasterxml.jackson.databind.JsonNode;
import com.jindle.Generator;
import com.jindle.model.Article;
import com.jindle.model.Book;
import com.jindle.model.Section;
import controllers.request.DeliveryRequest;
import controllers.request.FeedDeliveryRequest;
import de.jetwick.snacktory.HtmlFetcher;
import de.jetwick.snacktory.JResult;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import utils.SessionUtils;
import views.html.home;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
                    for (Map.Entry<String, List<Entry>> entries : unread.entrySet()) {
                        if (getFeedRequest(request, entries.getKey()).fullArticle) {
                            HtmlFetcher fetcher = new HtmlFetcher();
                            for (Entry entry : entries.getValue()) {
                                int timeOutMillis = 10 * 1000;
                                JResult result = fetcher.fetchAndExtract(entry.getUrl(), timeOutMillis, true);
                                if (result.getText() != null) {
                                    entry.setContent(result.getText());
                                }
                            }
                        }
                    }
                    Book.BookBuilder bookBuilder = Book.builder();
                    List<Section> sections = new ArrayList<>();
                    bookBuilder.title("radek");
                    for (FeedDeliveryRequest feed : request.feeds){
                        Section.SectionBuilder s = Section.builder();
                        s.title(feed.title);
                        List<Article> articles = new ArrayList<>();
                        for (Entry entry : unread.get(feed.id)){
                            Article.ArticleBuilder b = Article.builder();
                            b.author(entry.getAuthor());
                            b.title(entry.getTitle());
                            b.date(entry.getPublished());
                            b.content(entry.getContent());
                            articles.add(b.build());
                        }
                        s.articles(articles);
                        sections.add(s.build());
                    }
                    bookBuilder.sections(sections);
                    bookBuilder.language("en-gb").creator("keendly").subject("news").date("2015-01-01");
                    String s = new Generator("/tmp", "/home/radek/software/kindlegen/kindlegen").generate(bookBuilder.build());

                    return ok(s);
                });
    }

    private FeedDeliveryRequest getFeedRequest(DeliveryRequest deliveryRequest, String feedId){
        for (FeedDeliveryRequest feedDeliveryRequest : deliveryRequest.feeds){
            if (feedDeliveryRequest.id.equals(feedId)){
                return feedDeliveryRequest;
            }
        }
        return null;
    }

    private List<String> extractIds(List<FeedDeliveryRequest> feeds){
        return feeds.stream().map(feed -> feed.id).collect(Collectors.toList());
    }
}
