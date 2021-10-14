package me.june.chapter08.custom;

import me.june.chapter08.domain.Customer;
import org.springframework.batch.item.ItemProcessor;

/**
 * 스프링배치는 ItemProcessor 가 null 을 반환하면 해당 아이템을 필터링 함으로써 과정을 단순화한다. 짝수일 경우 필터링 하는 ItemProcessor
 */
public class EventFilteringItemProcessor implements ItemProcessor<Customer, Customer> {

    @Override
    public Customer process(Customer item) throws Exception {
        return Integer.parseInt(item.getZip()) % 2 == 0 ? null : item;
    }
}
