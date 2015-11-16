package models;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import java.util.Date;

@MappedSuperclass
public abstract class BaseEntity {

    @Column
    public Date created;

    @Column(name = "last_modified")
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
