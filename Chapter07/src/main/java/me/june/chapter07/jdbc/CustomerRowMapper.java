package me.june.chapter07.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import me.june.chapter07.domain.Customer;
import org.springframework.jdbc.core.RowMapper;

public class CustomerRowMapper implements RowMapper<Customer> {

    @Override
    public Customer mapRow(ResultSet rs, int rowNum) throws SQLException {
        Customer customer = new Customer();
        customer.setId(rs.getLong("id"));
        customer.setAddress(rs.getString("address"));
        customer.setCity(rs.getString("city"));
        customer.setFirstName(rs.getString("first_name"));
        customer.setLastName(rs.getString("last_name"));
        customer.setMiddleInitial(rs.getString("middle_initial"));
        customer.setState(rs.getString("state"));
        customer.setZipCode(rs.getString("zip_code"));
        return customer;
    }
}
