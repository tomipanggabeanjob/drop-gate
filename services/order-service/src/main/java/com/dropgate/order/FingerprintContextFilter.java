package com.dropgate.order;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class FingerprintContextFilter extends OncePerRequestFilter {
    private final FingerprintContext fingerprintContext;

    public FingerprintContextFilter(FingerprintContext fingerprintContext) {
        this.fingerprintContext = fingerprintContext;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        fingerprintContext.setFingerprint(request.getHeader("X-Fingerprint"));
        fingerprintContext.setUserAgent(request.getHeader("User-Agent"));
        fingerprintContext.setIpAddress(request.getRemoteAddr());
        filterChain.doFilter(request, response);
    }
}
