package me.june.chapter07.hibernate;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 기본적으로 Web 기반에서 애플리케이션에서 Hibernate 를 사용하는 경우와, 배치애플리케이션에서 사용하는 경우 성격이 좀 다르다. Web 기반의 경우 요청단위로 세션이
 * 생기고 끊어지지만, 배치애플리케이션의 경우 그렇지 않다. (100만건 처리를 한다면 100만건을 모두 처리할때 까지 세션이 유지되어 OOM 이 날것..) Spring
 * Batch 는 기본적으로 JdbcTransactionManager 를 사용한다. - DataSource <-> 하이버네이트 세션을 아우르는 TransactionManager
 * 가 필요 Hibernate 를 사용할경우 스프링이 제공하는 HibernateTransactionManager 를 이용해 이를 해소한다.
 */
public class HibernateBatchConfiguration extends DefaultBatchConfigurer {

    private DataSource dataSource;
    private SessionFactory sessionFactory;
    private PlatformTransactionManager transactionManager;

    public HibernateBatchConfiguration(DataSource dataSource,
        EntityManagerFactory entityManagerFactory) {
        super(dataSource);
        this.dataSource = dataSource;
        this.sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        this.transactionManager = new HibernateTransactionManager(this.sessionFactory);
    }

    @Override
    public PlatformTransactionManager getTransactionManager() {
        return this.transactionManager;
    }
}
