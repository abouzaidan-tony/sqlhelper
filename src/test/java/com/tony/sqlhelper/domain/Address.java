package com.tony.sqlhelper.domain;

import com.tony.sqlhelper.helper.SQLHelper.SQLTypes;
import com.tony.sqlhelper.annotation.OneToOne;
import com.tony.sqlhelper.annotation.PrimaryKey;
import com.tony.sqlhelper.annotation.Property;
import com.tony.sqlhelper.annotation.Table;

@Table("address")
public class Address {

    @PrimaryKey
    @Property(name="id", type = SQLTypes.Long)
    private Long id;

    @Property(name="full_address")
    private String fullAddress;

    @OneToOne(targetEntity = Customer.class, inverserdBy = "customer_id")
    private Customer customer;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullAddress() {
        return fullAddress;
    }

    public void setFullAddress(String fullAddress) {
        this.fullAddress = fullAddress;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
}