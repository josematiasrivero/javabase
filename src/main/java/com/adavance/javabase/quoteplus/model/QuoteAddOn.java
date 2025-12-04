package com.adavance.javabase.quoteplus.model;

import com.adavance.javabase.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "quote_add_ons")
@Getter
@Setter
public class QuoteAddOn extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "add_on_id", nullable = false)
    private AddOn addOn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "add_on_level_id")
    private AddOnLevel addOnLevel;

    @Column(nullable = false)
    private Boolean included = true;

    @Column(precision = 19, scale = 2)
    private BigDecimal customPrice;

    @Column(length = 500)
    private String notes;
}

