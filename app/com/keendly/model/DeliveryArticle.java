package com.keendly.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DeliveryArticle {
    public String id;
    public String url;
    public String title;
    public Long timestamp;
    public String author;
    public String content;
}
