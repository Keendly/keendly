package com.keendly.controllers.api.error;

public enum Error {

    DELIVERY_EMAIL_NOT_CONFIGURED("Send-to-Kindle email address not configured"),
    DELIVERY_SENDER_NOT_SET("Sender email address not set"),
    TOO_MANY_ITEMS("Max number of feeds in single delivery: %d"),
    WRONG_EMAIL("Email address incorrect, allowed domains: %s"),
    NO_ARTICLES("No articles found"),
    TOO_MANY_SUBSCRIPTIONS("Max number of active subscriptions: %d");

    private String message;

    Error(String message){
        this.message = message;
    }

    public String getMessage(){
        return message;
    }

}
