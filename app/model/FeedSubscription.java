package model;

import java.time.LocalTime;
import java.util.Date;
import java.util.Map;

public class FeedSubscription {

    public String providerId;
    public String title;
    public Map<LocalTime, Long> scheduled;
    public Date lastDelivery;
}
