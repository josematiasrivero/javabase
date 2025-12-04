package com.adavance.javabase.invoicing.model;

import com.adavance.javabase.model.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "test_lines")
@Getter
@Setter
public class Line extends BaseEntity {
    private Integer quantity;

    @ManyToOne
    private Item item;
    
    // Invoice relationship will be managed by Invoice's OneToMany list usually, 
    // but for bidirectional we can add it here.
    // However, GenericRestController might need help if we don't expose it properly.
    // Let's keep it simple first.
}
