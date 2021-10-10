package me.june.chapter07.entity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerRepositoryCustom {
    Page<Customer> findByCityQuerydsl(String city, Pageable pageable);
}
