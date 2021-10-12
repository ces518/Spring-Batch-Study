package me.june.chapter08.domain;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Data;

@Data
public class Customer {

    @NotNull(message = "First name is required")
    @Pattern(regexp = "[a-zA-z]", message = "First name must be alphabetical")
    private String firstName;

    @Size(min = 1, max = 1)
    @Pattern(regexp = "[a-zA-z]", message = "Middle initial must be alphabetical")
    private String middleInitial;

    @NotNull(message = "Last name is required")
    @Pattern(regexp = "[a-zA-z]", message = "Last name must be alphabetical")
    private String lastName;

    @NotNull(message = "Address is required")
    @Pattern(regexp = "[a-zA-z\\. ]+")
    private String address;

    @NotNull(message = "City is required")
    @Pattern(regexp = "[a-zA-z\\. ]+")
    private String city;

    @NotNull(message = "State is required")
    @Size(min = 2, max = 2)
    @Pattern(regexp = "[A-Z]{2}")
    private String state;

    /**
     * Size 와 Pattern 모두 적용한 이유 ? -> Pattern 만적용해도 요구사항은 만족할테지만, 각 애너테이션을 통해 고유 메세지 지정이 가능하다.
     * -> 또한 필드 값의 길이가 잘못되었는지, 형식이 잘못되었는지 식별이 가능하다는 장점이 있다.
     */
    @NotNull(message = "Zip is required")
    @Size(min = 5, max = 5)
    @Pattern(regexp = "\\d{5}")
    private String zip;
}
