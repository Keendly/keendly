package com.keendly.controllers.api.error;

public enum Error {

    DELIVERY_EMAIL_NOT_CONFIGURED("Send-to-Kindle email address not configured"),
    TOO_MANY_ITEMS("Max number of feeds in single delivery: %d");

    private String message;

    Error(String message){
        this.message = message;
    }

    public String getMessage(){
        return message;
    }

}
