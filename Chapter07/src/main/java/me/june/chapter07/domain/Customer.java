package me.june.chapter07.domain;

import java.util.List;
import lombok.Data;

@Data
public class Customer {

    private String middleInitial;
    private String firstName;
    private String lastName;
//    private String addressNumber;
//    private String street;
    private String address; // addressNumber + street 를 하나의 필드로..
    private String city;
    private String state;
    private String zipCode;

    private List<Transaction> transactions;

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        output.append(firstName);
        output.append(" ");
        output.append(middleInitial);
        output.append(". ");
        output.append(lastName);

        if (transactions != null && !transactions.isEmpty()) {
            output.append(" has ");
            output.append(transactions.size());
            output.append(" transactions. ");
        } else {
            output.append(" has no transactions.");
        }

        return output.toString();
    }
}
