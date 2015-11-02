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
import de.jetwick.snacktory.ArticleTextExtractor;
import de.jetwick.snacktory.JResult;
import mail.EmailSender;
import org.apache.http.HttpStatus;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.ws.WS;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.With;
import utils.ConfigUtils;
import utils.SessionUtils;
import views.html.home;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
                .flatMap(unread ->
                                fetchArticles(unread, request).map(ret -> {
                                    Book.BookBuilder bookBuilder = initBookBuilder(request);
                                    List<Section> sections = new ArrayList<>();
                                    for (FeedDeliveryRequest feed : request.feeds) {
                                        Section.SectionBuilder s = Section.builder();
                                        s.title(feed.title);
                                        List<Article> articles = new ArrayList<>();
                                        if (unread.containsKey(feed.id)){
                                            for (Entry entry : unread.get(feed.id)) {
                                                articles.add(convertToArticle(entry));
                                            }
                                            s.articles(articles);
                                            sections.add(s.build());
                                        }
                                    }
                                    bookBuilder.sections(sections);
                                    String filePath = new Generator(ConfigUtils.parameter("temp.directory"),
                                            ConfigUtils.parameter("kindlegen.path")).generate(bookBuilder.build());

                                    try {
                                        new EmailSender().sendFile(filePath, "moomeen@gmail.com");
                                    } catch (Exception e){
                                        return internalServerError(Json.toJson("error"));
                                    }

                                    adaptor.markAsRead(extractIdsToMarkAsRead(request.feeds), tokens).map(res -> {
                                      return "a";
                                    });
                                    return ok(Json.toJson("ok"));
                                })
                );
    }

    private Promise fetchArticles(Map<String, List<Entry>> entries, DeliveryRequest request){
        List<Promise<Entry>> promises = new ArrayList<>();
        for (Map.Entry<String, List<Entry>> entry : entries.entrySet()){
            if (getFeedRequest(request, entry.getKey()).fullArticle) {
                for (Entry entry1 : entry.getValue()) {
                    Promise<Entry> promise = fetchEntryContent(entry1);
                    promises.add(promise);
                }
            }
        }
        return Promise.sequence(promises);
    }

    private Promise<Entry> fetchEntryContent(Entry entry){
        return WS.url(entry.getUrl())
                .get().map(res -> {
                    if (res.getStatus() == HttpStatus.SC_OK){
                        ArticleTextExtractor extractor = new ArticleTextExtractor();
                        JResult result = extractor.extractContent(res.getBody());
                        entry.setContent(result.getText());
                    }
                    return entry;
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

    private List<String> extractIdsToMarkAsRead(List<FeedDeliveryRequest> feeds){
        return feeds.stream().filter(feed -> feed.markAsRead).map(feed -> feed.id).collect(Collectors.toList());
    }

    private Article convertToArticle(Entry entry){
        Article.ArticleBuilder b = Article.builder();
        b.author(entry.getAuthor());
        b.title(entry.getTitle());
        b.date(entry.getPublished());
        b.content(entry.getContent());
        return b.build();
    }

    private Book.BookBuilder initBookBuilder(DeliveryRequest request){
        Book.BookBuilder b = Book.builder();
        if (request.feeds.size() == 1){
            b.title(request.feeds.get(0).title);
        } else {
            b.title("Articles"); // TODO translated
        }
        b.language("en-gb").creator("keendly").subject("news").date(today());
        return b;
    }

    private String today(){
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }
}
