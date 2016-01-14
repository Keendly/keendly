package model;

import com.fasterxml.jackson.annotation.JsonInclude;
import entities.Provider;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class User {

    public Long id;
    public Provider provider;
    public String providerId;
    public String email;
    public String deliveryEmail;
}
