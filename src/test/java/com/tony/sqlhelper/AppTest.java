package com.tony.sqlhelper;

import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.List;

import com.tony.sqlhelper.domain.Address;
import com.tony.sqlhelper.domain.Author;
import com.tony.sqlhelper.domain.Book;
import com.tony.sqlhelper.domain.Customer;
import com.tony.sqlhelper.domain.PhoneNumber;
import com.tony.sqlhelper.proxy.ProxySQLObject;

import org.json.JSONException;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {
    /**
     * Rigorous Test :-)
     * 
     * @throws SQLException
     * @throws JSONException
     * @throws ClassNotFoundException
     */
    @Test
    public void shouldAnswerWithTrue() throws ClassNotFoundException, SQLException
    {
        //SQLHelper.GetInstance().getConnection();
        assertTrue( true );
    }

    @Test
    public void rollingBack() throws ClassNotFoundException, SQLException {
        Customer c = null;
       
        try{

            EntityManager em = EntityManager.GetInstance();
            Savepoint sp = null;
                
            em.beginTransaction();
            c = em.GetRepository(Customer.class).create();
            if (c instanceof ProxySQLObject)
                ((ProxySQLObject) c).getAccessNumber();
            c.setFirstName("Tony");
            c.setLastName("A. Z");
            Address address = em.GetRepository(Address.class).create();
            PhoneNumber p1 = em.GetRepository(PhoneNumber.class).create();
            p1.setPhoneNumber("1111");
            c.getPhoneNumbers().add(p1);
            address.setFullAddress("this is my full address");
            c.setAddress(address);
            em.persist(c);
            sp = em.setSavePoint();
            c = em.GetRepository(Customer.class).find(c.getId());
            assertTrue(c != null);
            c.setFirstName("Tony2");
            assertTrue(c.getAddress() != null);
            assertTrue(c.getPhoneNumbers().size() == 1);
            PhoneNumber p2 = em.GetRepository(PhoneNumber.class).create();
            p2.setPhoneNumber("2222");
            c.getPhoneNumbers().add(p2);
            em.persist(c);
            c = em.GetRepository(Customer.class).find(c.getId());
            assertTrue(c.getFirstName().equals("Tony2")); 
            assertTrue(c.getPhoneNumbers().size() == 2);
            assertTrue(c.getPhoneNumbers().get(0).getPhoneNumber().equals("1111"));
            assertTrue(c.getPhoneNumbers().get(1).getPhoneNumber().equals("2222"));

            em.rollBack(sp);

            c = em.GetRepository(Customer.class).find(c.getId());
            assertTrue(c != null);
            assertTrue(c.getFirstName().equals("Tony"));
            assertTrue(c.getPhoneNumbers().size() == 1);
            assertTrue(c.getPhoneNumbers().get(0).getPhoneNumber().equals("1111"));
            
            em.rollBack();
        
            c = em.GetRepository(Customer.class).find(c.getId());
            assertTrue(c == null);

        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    @Test
    public void commitRollbackTest() throws ClassNotFoundException, SQLException {
        Customer c = null;

        try {

            EntityManager em = EntityManager.GetInstance();
            Savepoint sp = null;

            em.beginTransaction();
            c = em.GetRepository(Customer.class).create();
            if (c instanceof ProxySQLObject)
                ((ProxySQLObject) c).getAccessNumber();
            c.setFirstName("Tony");
            c.setLastName("A. Z");
            Address address = em.GetRepository(Address.class).create();
            PhoneNumber p1 = em.GetRepository(PhoneNumber.class).create();
            p1.setPhoneNumber("1111");
            c.getPhoneNumbers().add(p1);
            address.setFullAddress("this is my full address");
            c.setAddress(address);
            em.persist(c);
            sp = em.setSavePoint();
            c = em.GetRepository(Customer.class).find(c.getId());
            assertTrue(c != null);
            c.setFirstName("Tony2");
            assertTrue(c.getAddress() != null);
            assertTrue(c.getPhoneNumbers().size() == 1);
            PhoneNumber p2 = em.GetRepository(PhoneNumber.class).create();
            p2.setPhoneNumber("2222");
            c.getPhoneNumbers().add(p2);
            em.persist(c);
            c = em.GetRepository(Customer.class).find(c.getId());
            assertTrue(c.getFirstName().equals("Tony2"));
            assertTrue(c.getPhoneNumbers().size() == 2);
            assertTrue(c.getPhoneNumbers().get(0).getPhoneNumber().equals("1111"));
            assertTrue(c.getPhoneNumbers().get(1).getPhoneNumber().equals("2222"));

            em.rollBack(sp);

            c = em.GetRepository(Customer.class).find(c.getId());
            assertTrue(c != null);
            assertTrue(c.getFirstName().equals("Tony"));
            assertTrue(c.getPhoneNumbers().size() == 1);
            assertTrue(c.getPhoneNumbers().get(0).getPhoneNumber().equals("1111"));

            em.commit();

            c = em.GetRepository(Customer.class).find(c.getId());
            assertTrue(c.getFirstName().equals("Tony"));
            assertTrue(c.getPhoneNumbers().size() == 1);
            assertTrue(c.getPhoneNumbers().get(0).getPhoneNumber().equals("1111"));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void getFromDb() throws ClassNotFoundException, SQLException {
        List<Customer> c = EntityManager.GetInstance().GetRepository(Customer.class).findAll();
        assertTrue( c.size() != 0);
    }

    @Test
    public void testSettersGetters() throws Exception{
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
    }

    @Test
    public void testManyToMany() throws Exception {
        Book b1 = EntityManager.GetInstance().GetRepository(Book.class).create();
        b1.setName("Book1");
        Book b2 = EntityManager.GetInstance().GetRepository(Book.class).create();
        b2.setName("Book2");

        Author a1 = EntityManager.GetInstance().GetRepository(Author.class).create();
        a1.setName("Author1");

        Author a2 = EntityManager.GetInstance().GetRepository(Author.class).create();
        a2.setName("Author2");

        a1.getBooks().add(b1);
        a1.getBooks().add(b2);

        a2.getBooks().add(b1);

        assertTrue(b1.getAuthors().size() == 2);

        assertTrue(b2.getAuthors().size() == 1);

        b1.getAuthors().remove(a1);

        assertTrue(a1.getBooks().size() == 1);
    }
}
