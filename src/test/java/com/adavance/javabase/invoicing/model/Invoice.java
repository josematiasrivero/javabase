package com.adavance.javabase.invoicing.model;

import com.adavance.javabase.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_invoices")
@Getter
@Setter
public class Invoice extends BaseEntity {
    private String number;
    private Instant date;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "invoice_id") // Unidirectional join for simplicity in test
    private List<Line> lines = new ArrayList<>();
}
