package com.tony.sqlhelper.domain;

import com.tony.sqlhelper.helper.SQLHelper.SQLTypes;

import java.util.List;

import com.tony.sqlhelper.annotation.OneToMany;
import com.tony.sqlhelper.annotation.OneToOne;
import com.tony.sqlhelper.annotation.PrimaryKey;
import com.tony.sqlhelper.annotation.Property;
import com.tony.sqlhelper.annotation.Table;

@Table("customer")
public class Customer {

    @PrimaryKey
    @Property(name = "id", type = SQLTypes.Long)
    private Long id;

    @Property(name = "firstname")
    private String firstName;

    @Property(name = "lastname")
    private String lastName;

    @OneToOne(targetEntity = Address.class, mappedBy = "customer_id")
    private Address address;

    @OneToMany(targetEntity = PhoneNumber.class, mappedBy="customer_id")
    private List<PhoneNumber> phoneNumbers;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public List<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }

    public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
    }
    
}