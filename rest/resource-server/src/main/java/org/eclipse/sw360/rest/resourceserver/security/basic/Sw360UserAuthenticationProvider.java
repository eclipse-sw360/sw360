/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.security.basic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.rest.resourceserver.security.TokenCapabilityAuthorities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class Sw360UserAuthenticationProvider implements AuthenticationProvider {


    private final Logger log = LogManager.getLogger(this.getClass());

    private final PasswordEncoder passwordEncoder;

    private final Sw360CustomUserDetailsService userDetailsService;

    @Autowired
    public Sw360UserAuthenticationProvider(@Lazy PasswordEncoder passwordEncoder, Sw360CustomUserDetailsService userDetailsService) {
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
    }

    /**
     * @param authentication the authentication request object.
     * @return
     * @throws AuthenticationException
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String userName = authentication.getName();
        String rawPassword;
        if (authentication.getCredentials() == null) {
            rawPassword = null;
        } else {
            rawPassword = authentication.getCredentials().toString();
        }
        log.info("Authenticating for the user with username {}", userName);

        UserDetails userDetails = userDetailsService.loadUserByUsername(userName);
        return checkPassword(userDetails, rawPassword);
    }

    private Authentication checkPassword(UserDetails userDetails, String rawPassword) {
        log.info("Checking password for user {}", userDetails.getUsername());
        if (passwordEncoder.matches(rawPassword, userDetails.getPassword())) {
            Set<GrantedAuthority> authorities = new HashSet<>(userDetails.getAuthorities());
            authorities.addAll(TokenCapabilityAuthorities.readWrite());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails,
                    rawPassword, authorities);
            if (userDetails instanceof Sw360UserDetails sw360UserDetails) {
                authentication.setDetails(sw360UserDetails.getSw360User());
            }
            return authentication;
        } else {
            throw new BadCredentialsException("Bad credentials");
        }
    }

    /**
     * @param authentication
     * @return
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
