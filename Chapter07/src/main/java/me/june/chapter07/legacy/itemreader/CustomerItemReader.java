package me.june.chapter07.legacy.itemreader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import me.june.chapter07.domain.Customer;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.stereotype.Component;

/**
 * CustomerService 를 ItemReader 로 변경
 * getCustomer() 를 ItemReader 인터페이스의 read() 메소드로 변경하기만 하면 된다..
 *
 * ItemReader 인터페이스는, **잡을 재실행 할때마다 모든 레코드를 다시 실행** 한다.
 * 만약 특정한 에러로 인해 잡이 중지되었다 재시작 되었을때, 에러가 발생했던 부분부터 다시 시작하려면, **잡의 상태** 를 기억해야 한다.
 * 이는 ItemStream 인터페이스를 구현해야 함을 의미한다.
 *
 * ItemStreamSupport 클래스를 상속..
 * 이는 getExecutionContextKey 라는 유틸리티 메소드를 제공 (컴포넌트 명으로 유일한 키를 생성한다.)
 */
public class CustomerItemReader extends ItemStreamSupport implements ItemReader<Customer> {
    private String INDEX_KEY = "current.index.customers";

    private List<Customer> customers;
    private int curIndex;

    private String[] firstNames = {"Michael", "Warren", "Ann", "Terrence",
        "Erica", "Laura", "Steve", "Larry"};
    private String middleInitial = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private String[] lastNames = {"Gates", "Darrow", "Donnelly", "Jobs",
        "Buffett", "Ellison", "Obama"};
    private String[] streets = {"4th Street", "Wall Street", "Fifth Avenue",
        "Mt. Lee Drive", "Jeopardy Lane",
        "Infinite Loop Drive", "Farnam Street",
        "Isabella Ave", "S. Greenwood Ave"};
    private String[] cities = {"Chicago", "New York", "Hollywood", "Aurora",
        "Omaha", "Atherton"};
    private String[] states = {"IL", "NY", "CA", "NE"};

    private Random generator = new Random();

    public CustomerItemReader() {
        curIndex = 0;

        customers = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            customers.add(buildCustomer());
        }
    }

    private Customer buildCustomer() {
        Customer customer = new Customer();

        customer.setId((long) generator.nextInt(Integer.MAX_VALUE));
        customer.setFirstName(
            firstNames[generator.nextInt(firstNames.length - 1)]);
        customer.setMiddleInitial(
            String.valueOf(middleInitial.charAt(
                generator.nextInt(middleInitial.length() - 1))));
        customer.setLastName(
            lastNames[generator.nextInt(lastNames.length - 1)]);
        customer.setAddress(generator.nextInt(9999) + " " +
            streets[generator.nextInt(streets.length - 1)]);
        customer.setCity(cities[generator.nextInt(cities.length - 1)]);
        customer.setState(states[generator.nextInt(states.length - 1)]);
        customer.setZipCode(String.valueOf(generator.nextInt(99999)));

        return customer;
    }

    @Override
    public Customer read() {
        Customer cust = null;

        if (curIndex < customers.size()) {
            cust = customers.get(curIndex);
            curIndex++;
        }

        return cust;
    }

    /**
     * 스프링 배치가 ItemReader 에서 필요한 상태 초기화시 호출한다.
     * 이전 상태를 복원 / 특정 파일을 열거나 특정 데이터베이스 연결 하는 등...
     */
    @Override
    public void open(ExecutionContext executionContext) {
        if (executionContext.containsKey(getExecutionContextKey(INDEX_KEY))) {
            int index = executionContext.getInt(getExecutionContextKey(INDEX_KEY));

            if (index == 50) {
                curIndex = 51;
            } else {
                curIndex = index;
            }
        } else {
            curIndex = 0;
        }
    }

    /**
     * 스프링 배치가 잡의 상태를 갱신할때 사용한다.
     */
    @Override
    public void update(ExecutionContext executionContext) {
        executionContext.putInt(getExecutionContextKey(INDEX_KEY), curIndex);
    }
}
