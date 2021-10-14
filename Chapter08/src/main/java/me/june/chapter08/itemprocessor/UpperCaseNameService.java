package me.june.chapter08.itemprocessor;

import me.june.chapter08.domain.Customer;

/**
 * 고객의 이름을 대문자로 변경하는 서비스
 */
public class UpperCaseNameService {

    public Customer upperCase(Customer customer) {
        Customer newCustomer = new Customer(customer);
        newCustomer.setFirstName(newCustomer.getFirstName().toUpperCase());
        newCustomer.setMiddleInitial(newCustomer.getMiddleInitial().toUpperCase());
        newCustomer.setLastName(newCustomer.getLastName().toUpperCase());
        return newCustomer;
    }
}
