package com.adavance.javabase.quoteplus.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "add_on_levels")
@Getter
@Setter
public class AddOnLevel extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "add_on_id", nullable = false)
    private AddOn addOn;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer levelOrder;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal priceModifier;

    @Column(nullable = false)
    private Boolean active = true;
}

