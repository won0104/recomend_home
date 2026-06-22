package com.ssafy.prj.security.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ssafy.prj.model.dto.UserAccount;
import com.ssafy.prj.model.service.UserAccountService;
import com.ssafy.prj.security.dto.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountService userAccountService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            UserAccount user = userAccountService.findByUserId(username);
            if (user == null) {
                throw new UsernameNotFoundException(username);
            }
            return new CustomUserDetails(user);
        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new UsernameNotFoundException(username, e);
        }
    }
}
