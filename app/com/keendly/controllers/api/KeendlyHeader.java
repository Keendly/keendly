package com.keendly.controllers.api;

public enum KeendlyHeader {

    AUTHORIZATION("Authorization"),
    NEW_TOKEN("X-Keendly-Set-Token"),
    SESSION_COOKIE("k33ndly_535510n");

    public String value;

    KeendlyHeader(String s){
        this.value = s;
    }
}
