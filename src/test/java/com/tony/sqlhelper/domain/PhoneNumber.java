package com.tony.sqlhelper.domain;

import com.tony.sqlhelper.annotation.ManyToOne;
import com.tony.sqlhelper.annotation.PrimaryKey;
import com.tony.sqlhelper.annotation.Property;
import com.tony.sqlhelper.annotation.Table;
import com.tony.sqlhelper.helper.SQLHelper.SQLTypes;

@Table("phone_number")
public class PhoneNumber{

    @PrimaryKey
    @Property(name="id", type = SQLTypes.Long)
    private Long id;

    @ManyToOne(targetEntity = Customer.class, inverserdBy = "customer_id")
    private Customer customer;

    @Property(name="phone_number")
    private String phoneNumber;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}