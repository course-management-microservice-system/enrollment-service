package com.nexusenroll.enrollment.db;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

@ApplicationScoped
public class DatabaseFactory {

    private EntityManagerFactory entityManagerFactory;

    @PostConstruct
    public void init() {
        entityManagerFactory = Persistence.createEntityManagerFactory("enrollment-pu");
    }

    @Produces
    public EntityManager createEntityManager() {
        return entityManagerFactory.createEntityManager();
    }

    @PreDestroy
    public void shutdown() {
        entityManagerFactory.close();
    }
}