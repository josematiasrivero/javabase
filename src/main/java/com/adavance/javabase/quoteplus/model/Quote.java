package com.adavance.javabase.quoteplus.model;

import com.adavance.javabase.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotes")
@Getter
@Setter
public class Quote extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String customerName;

    @Column
    private String customerEmail;

    @Column(length = 2000)
    private String notes;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private String status; // e.g., DRAFT, SENT, ACCEPTED, REJECTED

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuoteAddOn> selectedAddOns = new ArrayList<>();
}

