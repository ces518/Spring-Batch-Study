package me.june.chapter07.error.skip;

import java.io.FileNotFoundException;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ParseException;

/**
 * 스프링 배치는, 예외가 발생했을때, 스킵 처리를 위한 인터페이스인 SkipPolicy 를 제공한다.
 * 이는 대상 예외와 건너뛸 수 있는 횟수를 전달받는다.
 */
public class FileVerificationSkipper implements SkipPolicy {

    @Override
    public boolean shouldSkip(Throwable t, int skipCount) throws SkipLimitExceededException {
        if (t instanceof FileNotFoundException) {
            return false;
        } else if (t instanceof ParseException && skipCount <= 10) {
            return true;
        } else {
            return false;
        }
    }
}
