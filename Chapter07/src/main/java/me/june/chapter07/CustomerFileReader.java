package me.june.chapter07;

import java.util.ArrayList;
import me.june.chapter07.domain.Customer;
import me.june.chapter07.domain.Transaction;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

/**
 * ItemReader 인터페이스가 아닌 ItemStreamReader 인터페이스를 구현했다.
 * - 이는 ItemReader / ItemStream 를 Combined 한 Convenience Interface
 */
public class CustomerFileReader implements ItemStreamReader<Customer> {

    private Object currentItem = null;

    private ItemStreamReader<Object> delegate;

    public CustomerFileReader(ItemStreamReader<Object> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Customer read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        // 고객 정보 읽기
        if (currentItem == null) {
            this.currentItem = delegate.read();
        }
        Customer customer = (Customer) currentItem;
        currentItem = null;

        if (customer != null) {
            customer.setTransactions(new ArrayList<>());

            // 거래정보 읽기
            // 만약 다음 고객 레코드가 발견되면 현재 고객 레코드가 끝난것으로 간주
            // 이런 로직을 제어 중지 로직 (control break logic) 이라고한다.
            while (peek() instanceof Transaction) {
                customer.getTransactions().add((Transaction) currentItem);
                currentItem = null;
            }
        }
        return customer;
    }

    /**
     * 스프링 배치가 제공해주는 ItemReader 의 구현체를 사용하면, ExecutionContext 를 이용한 리소스 관리를 해준다.
     * 하지만 직접 ItemReader 를 구현할 경우 이런 리소스 관리까지 직접 처리해주어야 한다.
     * 예제는 ItemReader 구현체를 래핑해서 사용하기 때문에 delegate  이용해서 이를 대신 처리하게 끔 한다.
     */
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        delegate.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        delegate.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        delegate.close();
    }

    private Object peek() throws Exception {
        if (currentItem == null) {
            currentItem = delegate.read();
        }
        return currentItem;
    }
}
