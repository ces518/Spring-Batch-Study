package me.june.chapter06.csv.domain;

import lombok.Data;

@Data
public class AccountSummary {

    private int id;
    private String accountNumber;
    private Double currentBalance;
}
