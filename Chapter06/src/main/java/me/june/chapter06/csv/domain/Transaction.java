package me.june.chapter06.csv.domain;

import java.util.Date;
import lombok.Data;

@Data
public class Transaction {

    private String accountNumber;
    private Date timestamp;
    private double amount;
}
