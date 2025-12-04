package com.adavance.javabase.quoteplus.model;

import com.adavance.javabase.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "product_add_ons")
@Getter
@Setter
public class ProductAddOn extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "add_on_id", nullable = false)
    private AddOn addOn;

    @Column(nullable = false)
    private Boolean includedByDefault = false;

    @Column(nullable = false)
    private Boolean required = false;

    @Column(precision = 19, scale = 2)
    private BigDecimal customPrice;

    @Column(nullable = false)
    private Integer displayOrder = 0;
}

