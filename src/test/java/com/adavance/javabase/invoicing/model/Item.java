package com.adavance.javabase.invoicing.model;

import com.adavance.javabase.model.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "test_items")
@Getter
@Setter
public class Item extends BaseEntity {
    private String name;
    private String code;
    private BigDecimal price;
}
