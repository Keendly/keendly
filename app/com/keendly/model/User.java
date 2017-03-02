package com.keendly.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.keendly.entities.Provider;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class User {

    public Long id;
    public Provider provider;
    public String providerId;
    public String email;
    public String deliveryEmail;
    public String deliverySender;
}
