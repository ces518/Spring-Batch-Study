package me.june.chapter09.jdbc.preparedstatement;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import me.june.chapter09.domain.Customer;
import org.springframework.batch.item.database.ItemPreparedStatementSetter;

public class CustomerItemPreparedStatementSetter implements ItemPreparedStatementSetter<Customer> {

    @Override
    public void setValues(Customer item, PreparedStatement ps) throws SQLException {
        ps.setString(1, item.getFirstName());
        ps.setString(2, item.getMiddleInitial());
        ps.setString(3, item.getLastName());
        ps.setString(4, item.getAddress());
        ps.setString(5, item.getCity());
        ps.setString(6, item.getState());
        ps.setString(7, item.getZip());
    }
}
