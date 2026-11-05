package com.modestoneto.secretaria.application.port.out;

public interface PasswordHasherPort {

    boolean matches(String rawPassword, String passwordHash);
}
