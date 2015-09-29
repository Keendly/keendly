package adaptors.auth;

public class Subscription {

    private String title;
    private String feedId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFeedId() {
        return feedId;
    }

    public void setFeedId(String id) {
        this.feedId = id;
    }
}
