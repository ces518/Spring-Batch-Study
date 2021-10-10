package me.june.chapter07.querydsl.queryprovider;

import static me.june.chapter07.entity.QCustomer.customer;

import com.querydsl.jpa.impl.JPAQueryFactory;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.springframework.batch.item.database.orm.AbstractJpaQueryProvider;
import org.springframework.util.Assert;

public class QuerydslQueryProvider extends AbstractJpaQueryProvider {

    private JPAQueryFactory queryFactory;
    private String cityName;

    public QuerydslQueryProvider(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public Query createQuery() {
        return queryFactory
            .select(customer)
            .from(customer)
            .where(customer.city.eq(cityName))
            .createQuery();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(cityName, "City name is required");
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }
}
