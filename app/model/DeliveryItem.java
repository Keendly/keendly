package model;

import java.util.List;

public class DeliveryItem {

    public Long id;
    public String feedId;
    public String title;
    public Boolean includeImages;
    public Boolean fullArticle;
    public Boolean markAsRead;
    public List<DeliveryArticle> articles;
}
