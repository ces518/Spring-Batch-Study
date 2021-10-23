package me.june.chapter09.hibernate;

import java.util.Collections;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class HibernateBatchConfigurer extends DefaultBatchConfigurer {

    private DataSource dataSource;
    private SessionFactory sessionFactory;
    private JobRepository jobRepository;
    private PlatformTransactionManager transactionManager;
    private JobLauncher jobLauncher;
    private JobExplorer jobExplorer;

    public HibernateBatchConfigurer(DataSource dataSource,
        EntityManagerFactory entityManagerFactory) {
        this.dataSource = dataSource;
        this.sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        this.transactionManager = new HibernateTransactionManager(sessionFactory);
    }

    @Override
    public PlatformTransactionManager getTransactionManager() {
        return this.transactionManager;
    }
}