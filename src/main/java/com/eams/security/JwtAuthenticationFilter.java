package com.eams.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Reads "Authorization: Bearer <jwt>" and logs the user in for this request.
 *
 * The token is rejected (request continues as anonymous -> 401) when:
 *  - signature/expiry is invalid
 *  - the user no longer exists
 *  - the user is disabled or their employee profile is INACTIVE
 *  - the password was changed after the token was issued (token version mismatch)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null
                && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            String token = header.substring(7);
            Optional<Claims> claims = jwtService.parse(token);

            if (claims.isPresent()) {
                authenticate(claims.get(), request);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(Claims claims, HttpServletRequest request) {
        AppUserPrincipal principal;
        try {
            principal = userDetailsService.loadUserByUsername(claims.getSubject());
        } catch (UsernameNotFoundException e) {
            return;
        }

        if (!principal.isEnabled()) {
            return;
        }

        Number tokenVersion = claims.get(JwtService.CLAIM_TOKEN_VERSION, Number.class);
        int versionInToken = tokenVersion == null ? 0 : tokenVersion.intValue();
        if (versionInToken != principal.tokenVersion()) {
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
