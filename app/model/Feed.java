package model;

import java.util.List;

public class Feed {

    public String feedId;
    public String title;
    public List<Subscription> subscriptions;
    public Delivery lastDelivery;
}
