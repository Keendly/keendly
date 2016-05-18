package com.keendly.entities;

import javax.persistence.*;
import java.util.Date;

@MappedSuperclass
public abstract class BaseEntity {

    @Column(columnDefinition= "timestamp with time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Date created;

    @Column(name = "last_modified", columnDefinition= "timestamp with time zone")
    public Date lastModified;

    @PrePersist
    public void prePersist(){
        Date d = new Date();
        created = d;
        lastModified = d;
    }

    @PreUpdate
    public void preUpdate(){
        lastModified = new Date();
    }
}
