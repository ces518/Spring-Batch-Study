package me.june.chapter09.composite;

import me.june.chapter09.domain.Customer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.classify.Classifier;

public class CustomerClassifier implements Classifier<Customer, ItemWriter<? super Customer>> {

    private ItemWriter<Customer> fileItemWriter;
    private ItemWriter<Customer> jpaItemWriter;

    public CustomerClassifier(
        ItemWriter<Customer> fileItemWriter,
        ItemWriter<Customer> jpaItemWriter
    ) {
        this.fileItemWriter = fileItemWriter;
        this.jpaItemWriter = jpaItemWriter;
    }

    @Override
    public ItemWriter<? super Customer> classify(Customer customer) {
        if (customer.getState().matches("^[A-M].*")) {
            return fileItemWriter;
        }
        return jpaItemWriter;
    }
}
