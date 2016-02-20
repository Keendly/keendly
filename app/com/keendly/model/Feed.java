package com.keendly.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Feed {

    public String feedId;
    public String title;
    public List<Subscription> subscriptions;
    public Delivery lastDelivery;
}
