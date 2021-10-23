package me.june.chapter09.custom;

import me.june.chapter09.domain.Customer;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    public void logCustomer(Customer customer) {
        System.out.println("I just saved" + customer);
    }

    public void logCustomerAddress(String address, String city, String state, String zip) {
        System.out.println(
            String.format("I just saved the address: \n %s \n %s \n %s \n %s", address, city, state, zip)
        );
    }
}
