package models;

import javax.persistence.*;

@Entity
@Table(name = "KeendlyUser", uniqueConstraints = @UniqueConstraint(columnNames = {"provider", "provider_id"}))
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    public long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public Provider provider;

    @Column(name = "provider_id", nullable = false)
    public String providerId;

    @Column
    public String email;

    @Column(name = "delivery_email")
    public String deliveryEmail;
}
