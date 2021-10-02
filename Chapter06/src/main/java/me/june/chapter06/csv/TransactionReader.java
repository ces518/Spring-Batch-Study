package me.june.chapter06.csv;

import me.june.chapter06.csv.domain.Transaction;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.file.transform.FieldSet;

/**
 * 스프링 배치는 ItemReader, ItemProcessor, ItemWriter, ItemStream 구현체인지 확인하고 적절한 시점에 콜백이 되도록 수행한다.
 * 스프링 배치에 명시적으로 등록되어 있지 않다면, 위 인터페이스들을 구현하고 있는지 확인하지 않는다.
 * -> 이런 경우 선택가능한 옵션은 두가지
 * -> 1. 잡에서 리더를 ItemStream 으로 명시적으로 등록하는 방법
 * -> 2. ItemStream 을 구현하고 적절한 라이프사이클에 따라 메소드를 호출하는 방법
 */
public class TransactionReader implements ItemStreamReader<Transaction> {

    private ItemStreamReader<FieldSet> fieldSetReader;
    private int recordCount = 0;
    private int expectedRecordCount = 0;

    private StepExecution stepExecution;

    public TransactionReader(ItemStreamReader<FieldSet> fieldSetReader) {
        this.fieldSetReader = fieldSetReader;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        fieldSetReader.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        fieldSetReader.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        fieldSetReader.close();
    }

    @Override
    public Transaction read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        /**
         * 임의로 예외를 발생시킴..
         * this.stepExecution.setTerminateOnly() => 스텝이 완료된 후 잡이 중지 (ExitStatus.STOPPED)
         * 예외를 방생시키는 방식은 스탭이 완료되지 않았다는 점 (ExitStatus.FAILED) 이다.
         * 스탭이 FAILED 로 식별되면, 어떤 청크 처리중 중단되었는지 기록되어있기 때문에, 해당 부분부터 재시작이 가능하다.
         */

        if (this.recordCount == 25) {
            throw new ParseException("This isn't what i hoped to happen");
        }
        return process(fieldSetReader.read());
    }

    private Transaction process(FieldSet fieldSet) {
        Transaction result = null;

        if (fieldSet != null) {
            if (fieldSet.getFieldCount() > 1) { // 레코드에 값이 2개이상 이라면 데이터 레코드, 아닐경우 푸터 레코드이다.
                result = new Transaction();
                result.setAccountNumber(fieldSet.readString(0));
                result.setTimestamp(fieldSet.readDate(1, "yyyy-MM-DD HH:mm:ss"));
                result.setAmount(fieldSet.readDouble(2));
                recordCount++;
            } else {
                expectedRecordCount = fieldSet.readInt(0);

                /**
                 * AfterStep 에 있던 Record 검사로직이 process 로 이동
                 */
                // 푸터 레코드와 실제 레코드 값이 다를경우 스탭이 완료된 후 스프링배치가 종료되도록 지시
                // JOB 이 STOPPED 상태를 반환하는 대신 스프링 배치가 JobInterruptedException 을 던진다.
                if (expectedRecordCount != this.recordCount) {
                    this.stepExecution.setTerminateOnly();
                }
            }
        }
        return result;
    }

    /**
     * 스프링배치에서 CSV 리더를 제공하지만, 커스텀 리더를 만든 이유 ?
     * -> 스탭의 ExitStatus 가 리더의 상태에 묶여 있기 때문이다..
     * -> Footer 레코드에 기록된 수와 실제로 읽은 레코드 수가 다르다면, 잡의 실행을 계속해선 안된다.
     * -> Footer 레코드에 도달했을때, 예상 레코드수와 실제 레코드수가 다르다면 STOPPED 를 반환해서 처리를 중단한다.
     */
//    @AfterStep
//    public ExitStatus afterStep(StepExecution execution) {
//        if (recordCount == expectedRecordCount) {
//            return execution.getExitStatus();
//        } else {
//            return ExitStatus.STOPPED;
//        }
//    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }
}
