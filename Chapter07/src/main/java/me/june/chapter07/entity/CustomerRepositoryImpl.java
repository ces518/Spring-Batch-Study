package me.june.chapter07.entity;

import static me.june.chapter07.entity.QCustomer.customer;

import me.june.chapter07.entity.QCustomer;
import me.june.chapter07.querydsl.QuerydslRepositorySupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class CustomerRepositoryImpl extends QuerydslRepositorySupport implements CustomerRepositoryCustom {

    public CustomerRepositoryImpl() {
        super(Customer.class);
    }

    @Override
    public Page<Customer> findByCityQuerydsl(String city, Pageable pageable) {
        return applyPagination(pageable, contentQuery -> contentQuery.selectFrom(customer)
            .where(customer.city.eq(city)));
    }
}
