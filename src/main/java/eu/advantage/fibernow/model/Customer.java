package eu.advantage.fibernow.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.Set;

@Data
@Builder
public class Customer {
    private Long id;
    private String tin;
    private String firstname;
    private String lastname;
    private String address;
    private Set<String> phoneNumber;
    private Set<String> email;
    private String username;
    private String password;
    private Status status;
}