package me.june.chapter09.multiresource;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import me.june.chapter09.domain.Customer;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.batch.item.file.FlatFileFooterCallback;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class CustomerRecordCountFooterCallback implements FlatFileFooterCallback {

    private int itemsWritteInCurrentFile = 0;

    @Override
    public void writeFooter(Writer writer) throws IOException {
        writer.write("This file contains" + itemsWritteInCurrentFile + "items");
    }

    /**
     * @Aspect 를 적용하는 이유 ?
     * -> MultiResourceItemWriter.write 호출 하기 전 Listener.write 가 호출된다.
     * 하지만 FlatFileItemWriter.open 에 대한 호출은 MultiResourceItemWriter.write 내부에서 이뤄짐
     * 때문에 FlatFileItemWriter.write 호출전 카운터를 초기화 하는 용도
     */
    @Before("execution(* org.springframework.batch.item.support.AbstractFileItemWriter.open(..))")
    public void resetCounter() {
        this.itemsWritteInCurrentFile = 0;
    }

    @Before("execution(* org.springframework.batch.item.support.AbstractFileItemWriter.write(..))")
    public void beforeWrite(JoinPoint joinPoint) {
        List<Customer> items = (List<Customer>) joinPoint.getArgs()[0];
        this.itemsWritteInCurrentFile += items.size();
    }
}
