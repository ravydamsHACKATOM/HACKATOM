package com.studyflow.controller;

import com.studyflow.dto.LoginRequest;
import com.studyflow.dto.ProfileUpdateRequest;
import com.studyflow.dto.RegisterRequest;
import com.studyflow.model.UserProfile;
import com.studyflow.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(UserRepository users, AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository) {
        this.users = users;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    @PostMapping("/register")
    @Transactional
    public UserProfile register(@Valid @RequestBody RegisterRequest request) {
        if (users.emailExists(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este e-mail já está cadastrado.");
        }
        boolean firstAccount = users.userCount() == 0;
        long id = users.create(request);
        if (firstAccount) {
            users.assignLegacyRecords(id);
        }
        return users.findByEmail(request.email()).orElseThrow();
    }

    @PostMapping("/login")
    public UserProfile login(@Valid @RequestBody LoginRequest request,
                             HttpServletRequest servletRequest,
                             HttpServletResponse servletResponse) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(),
                            request.password()));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            if (servletRequest.getSession(false) != null) {
                servletRequest.changeSessionId();
            }
            securityContextRepository.saveContext(context, servletRequest, servletResponse);
            return users.findByEmail(authentication.getName()).orElseThrow();
        } catch (BadCredentialsException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "E-mail ou senha incorretos.");
        }
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @GetMapping("/me")
    public UserProfile me(Authentication authentication) {
        return users.findByEmail(authentication.getName()).orElseThrow();
    }

    @PutMapping("/profile")
    public UserProfile updateProfile(@Valid @RequestBody ProfileUpdateRequest request,
                                     Authentication authentication) {
        return users.updateName(authentication.getName(), request).orElseThrow();
    }
}
