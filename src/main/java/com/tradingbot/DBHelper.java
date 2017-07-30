package com.tradingbot;

import com.tradingbot.entities.Discount;
import com.tradingbot.entities.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.util.List;

public class DBHelper {

    private static SessionFactory sessionFactory = null;

    static {
        if(sessionFactory == null) {
            StandardServiceRegistry registry = new StandardServiceRegistryBuilder().configure().build();
            try {
                sessionFactory = new MetadataSources(registry).buildMetadata().buildSessionFactory();
            } catch (Exception e) {
                StandardServiceRegistryBuilder.destroy(registry);
            }
        }
    }

    public static synchronized void addUser(User newUser) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        session.save(newUser);
        session.flush();
        transaction.commit();
        session.close();
    }

    public static synchronized boolean isUserExists(String username) {
        Session session = sessionFactory.openSession();
        List list = session.createQuery("from User where userName='" + username + "'").list();
        session.close();
        return !list.isEmpty();
    }

    public static synchronized void updateUser(String username, UserHandler userHandler) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        List<User> list = session.createQuery("from User where userName='" + username + "'").list();
        if(!list.isEmpty()) {
            userHandler.handle(list.get(0));
        }
        session.flush();
        transaction.commit();
        session.close();
    }

    public static synchronized void updateUser(User user) {
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        session.saveOrUpdate(user);
        session.flush();
        transaction.commit();
        session.close();
    }

    public static synchronized User getUser(String username) {
        Session session = sessionFactory.openSession();
        List<User> list = session.createQuery("from User where userName='" + username + "'").list();
        session.close();
        if(!list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }
}
