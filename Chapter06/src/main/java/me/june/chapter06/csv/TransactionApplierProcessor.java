package me.june.chapter06.csv;

import java.util.List;
import me.june.chapter06.csv.domain.AccountSummary;
import me.june.chapter06.csv.domain.Transaction;
import me.june.chapter06.csv.domain.TransactionDao;
import org.springframework.batch.item.ItemProcessor;

public class TransactionApplierProcessor implements ItemProcessor<AccountSummary, AccountSummary> {

    private TransactionDao transactionDao;

    public TransactionApplierProcessor(TransactionDao transactionDao) {
        this.transactionDao = transactionDao;
    }

    @Override
    public AccountSummary process(AccountSummary accountSummary) throws Exception {
        List<Transaction> transactions = transactionDao.getTransactionsByAccountNumber(
            accountSummary.getAccountNumber());

        for (Transaction transaction : transactions) {
            accountSummary.setCurrentBalance(accountSummary.getCurrentBalance() + transaction.getAmount());
        }
        return accountSummary;
    }
}
