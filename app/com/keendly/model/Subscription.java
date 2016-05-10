package com.keendly.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Subscription {

    public Long id;
    public String time;
    public String timezone;
    public List<DeliveryItem> feeds;
    public User user;
}
