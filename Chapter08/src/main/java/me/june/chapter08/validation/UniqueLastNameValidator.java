package me.june.chapter08.validation;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import me.june.chapter08.domain.Customer;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.batch.item.validator.Validator;

/**
 * ItemStreamSupport 클래스를 통해 ItemStream 인터페이스를 구현 lastNames 상태를 ExecutionContext 를 통해 관리한다.
 */
public class UniqueLastNameValidator extends ItemStreamSupport implements Validator<Customer> {

    private Set<String> lastNames = new HashSet<>();

    @Override
    public void validate(Customer value) throws ValidationException {
        if (lastNames.contains(value.getLastName())) {
            throw new ValidationException("Duplicate last name was found : " + value.getLastName());
        }
        lastNames.add(value.getLastName());
    }

    /**
     * ExecutionContext 를 통해 기존의 LastNames 를 가져온다.
     */
    @Override
    public void open(ExecutionContext executionContext) {
        String lastNames = getExecutionContextKey("lastNames");
        if (executionContext.containsKey(lastNames)) {
            this.lastNames = (Set<String>) executionContext.get(lastNames);
        }
    }

    /**
     * ExecutionContext 를 통해 LastNames 를 저장한다.
     */
    @Override
    public void update(ExecutionContext executionContext) {
        Iterator<String> iter = lastNames.iterator();
        Set<String> copiedLastNames = new HashSet<>();
        while (iter.hasNext()) {
            copiedLastNames.add(iter.next());
        }
        executionContext.put(getExecutionContextKey("lastNames"), copiedLastNames);
    }
}
