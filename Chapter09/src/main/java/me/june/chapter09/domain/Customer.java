package me.june.chapter09.domain;

import java.io.Serializable;
import lombok.Data;

@Data
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    private long id;
    private String firstName;
    private String middleInitial;
    private String lastName;
    private String address;
    private String city;
    private String state;
    private String zip;

}
