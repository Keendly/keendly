package com.keendly.model;

import java.util.List;

public class DeliveryRequest {

    public Long id;
    public Long userId;
    public String email;
    public Long timestamp;

    public List<DeliveryItem> items;
    public S3Object s3Items;
    public boolean dryRun;
}
