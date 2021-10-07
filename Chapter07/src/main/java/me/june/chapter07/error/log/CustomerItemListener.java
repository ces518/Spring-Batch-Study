package me.june.chapter07.error.log;

import javax.batch.api.chunk.listener.ItemReadListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.annotation.OnReadError;
import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.batch.item.file.FlatFileParseException;

/**
 * 로그를 남기게끔 리스너를 사용하는 방법은 3가지
 * 1. ItemReadListener 인터페이스 구현
 * 2. ItemListenerSupport 클래스 상속 후 onReadError 메소드 구현
 * 3. @OnReadError 애노테이션을 사용한 POJO 사용
 *
 * @see org.springframework.batch.core.ItemReadListener
 * @see org.springframework.batch.core.listener.ItemListenerSupport
 */
@Slf4j
public class CustomerItemListener {

    @OnReadError
    public void onReadError(Exception e) {
        if (e instanceof FlatFileParseException) {
            FlatFileParseException ffpe = (FlatFileParseException) e;

            StringBuilder errorsMessage = new StringBuilder();
            errorsMessage.append("An error occured while processing the " + ffpe.getLineNumber() + " line of the file. Below was the faulty input.\n");
            errorsMessage.append(ffpe.getInput() +"\n");
            log.error(errorsMessage.toString(), ffpe);
        } else {
            log.error("An errors has occured", e);
        }

    }
}
