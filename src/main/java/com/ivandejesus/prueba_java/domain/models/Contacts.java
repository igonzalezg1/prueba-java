package com.ivandejesus.prueba_java.domain.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class Contacts {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private int zipCode;
    private String address;
}
