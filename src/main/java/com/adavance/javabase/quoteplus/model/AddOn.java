package com.adavance.javabase.quoteplus.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "add_ons")
@Getter
@Setter
public class AddOn extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false)
    private Boolean hasLevels = false;

    @Column(nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "addOn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AddOnLevel> levels = new ArrayList<>();

    @OneToMany(mappedBy = "addOn", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductAddOn> productAddOns = new ArrayList<>();
}

