package controllers;

public enum KeendlyHeader {

    PROVIDER("X-Keendly-Provider"),
    PROVIDER_ID("X-Keendly-Provider-Id"),
    ACCESS_TOKEN("X-Keendly-Access-Token"),
    REFRESH_TOKEN("X-Keendly-Refresh-Token");

    public String value;

    KeendlyHeader(String s){
        this.value = s;
    }
}
