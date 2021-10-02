package me.june.chapter06.csv.domain;

import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

public class TransactionDao extends JdbcTemplate {

    private static final String GET_BY_ACCOUNT_NUMBER = "select t.id, t.timestamp, t.amount" +
        "from Transaction t join Account_Summary a on" +
        "a.id = t.account_summary_id" +
        "where a.account_number = ?";

    public TransactionDao(DataSource dataSource) {
        super(dataSource);
    }

    public List<Transaction> getTransactionsByAccountNumber(String accountNumber) {
        return queryForList(GET_BY_ACCOUNT_NUMBER, Transaction.class, accountNumber);
//        return query(
//            GET_BY_ACCOUNT_NUMBER,
////            (rs, rowNum) -> {
////                Transaction transaction = new Transaction();
////                transaction.setAmount(rs.getDouble("amount"));
////                transaction.setTimestamp(rs.getDate("timestamp"));
////                return transaction;
////            },
//            BeanPropertyRowMapper.newInstance(Transaction.class), // snake_case to camel_case 자동 매핑해줌
//            accountNumber);
    }
}
