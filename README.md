# SQLHELPER

## Description

SQLHelper is a library used  as a DB abstraction for java. To map DB table into java objects

SQLHelper takes care of relations between table and bind java objects based on that relation.

It supports transactions and caching.

## Usage

```Java
@Table("address") //sql table name
public class Address {

    @PrimaryKey
    @Property(name="id", type = SQLTypes.Long) //name is the column
    private Long id;

    @Property(name="full_address")
    private String fullAddress;

    @OneToOne(targetEntity = Customer.class, inverserdBy = "customer_id")
    //table address has column customer_id referencing customer table.
    private Customer customer;

    //setters and getters
}
```

```Java
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

    //setters and getters
}
```

### Initialization of instance

to create an object of your class use:

```Customer c = EntityManager.GetInstance().GetRepository(Customer.class).create();```

### Setters and Getters

if you call :  ```customer.setAddress(address);```, **automatically**  ```address.getCustomer()``` will be equal to ```customer```

and that is valid for all relations:
    one to one
    one to many
    many to one
    many to many

As of examples:

```Java
Customer c = EntityManager.GetInstance().GetRepository(Customer.class).create();
Address a = EntityManager.GetInstance().GetRepository(Address.class).create();
c.setAddress(a);
assertTrue(a.getCustomer() != null);
PhoneNumber p = EntityManager.GetInstance().GetRepository(PhoneNumber.class).create();
p.setCustomer(c);
assertTrue(c.getPhoneNumbers().size() == 1);
p.setCustomer(null);
assertTrue(c.getPhoneNumbers().size() == 0);
c.getPhoneNumbers().add(p);
assertTrue(p.getCustomer() != null);
```

### Transactions

#### Starting Transaction

To begin a transaction use:

```Java
    EntityManager.GetInstance().beginTransaction();
```

#### Persistance

To persist objects call:

```Java
 EntityManager.GetInstance().persist(object);
```

To delete objects:

```Java
EntityManager.GetInstance().remove(object);
```

To commit:

```Java
EntityManager.GetInstance().commit();
```

To create savepoint:

```Java
Savepoint sp = EntityManager.GetInstance().setSavePoint();
```

To rollback:

```Java
EntityManager.GetInstance().rollBack(sp); //Rollback to SAVEPOINT
//OR
EntityManager.GetInstance().rollBack();
```
