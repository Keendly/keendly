package com.keendly.entities;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "UserNotification")
public class UserNotificationEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public long id;

    @Column(columnDefinition= "timestamp with time zone")
    @Temporal(TemporalType.TIMESTAMP)
    public Date sendDate;

    @Column(nullable = false)
    public String type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    public UserEntity user;
}
