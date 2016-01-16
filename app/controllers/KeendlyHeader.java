package controllers;

public enum KeendlyHeader {

    AUTHORIZATION("Authorization"),
    NEW_TOKEN("X-Keendly-Set-Token");

    public String value;

    KeendlyHeader(String s){
        this.value = s;
    }
}
