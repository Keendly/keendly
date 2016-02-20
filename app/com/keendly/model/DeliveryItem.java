package com.keendly.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DeliveryItem {

    public Long id;
    public String feedId;
    public String title;
    public Boolean includeImages;
    public Boolean fullArticle;
    public Boolean markAsRead;
    public List<DeliveryArticle> articles;
}
