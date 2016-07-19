package com.keendly.model;

import java.util.List;

public class DeliveryRequest {

    public Long id;
    public Long userId;
    public String email;
    public Long timestamp;

    public List<DeliveryItem> items;
}