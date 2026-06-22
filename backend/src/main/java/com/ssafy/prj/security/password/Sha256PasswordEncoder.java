package com.ssafy.prj.security.password;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.ssafy.prj.util.PasswordUtil;

public class Sha256PasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        return PasswordUtil.sha256(rawPassword == null ? "" : rawPassword.toString());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null) {
            return false;
        }
        return encodedPassword.equals(encode(rawPassword));
    }
}
