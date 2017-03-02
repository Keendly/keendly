package com.keendly.model;

import com.keendly.entities.Provider;

import java.util.List;

public class DeliveryRequest {

    public Long id;
    public Long userId;
    public Provider provider;
    public String sender;
    public String email;
    public Long timestamp;

    public List<DeliveryItem> items;
    public S3Object s3Items;
    public boolean dryRun;
}
