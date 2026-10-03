package br.com.demosdd.auth;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.demosdd.shared.web.ApiException;
import br.com.demosdd.user.User;
import br.com.demosdd.user.UserRepository;
import br.com.demosdd.user.UserStatus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Login com e-mail e senha (RF06, RN04), na ordem do design D2: quem não sabe a
 * senha nunca descobre se o e-mail existe ou se a conta está pendente.
 */
@Service
class LoginService {

    static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    static final String ACCOUNT_PENDING = "ACCOUNT_PENDING";

    private static final List<SimpleGrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SessionAuthenticationStrategy sessionStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();
    /** Conferido quando o e-mail não existe, para o tempo de resposta não revelar isso. */
    private final String dummyHash;

    LoginService(UserRepository users, PasswordEncoder passwordEncoder, SessionAuthenticationStrategy sessionStrategy,
                 SecurityContextRepository securityContextRepository) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.sessionStrategy = sessionStrategy;
        this.securityContextRepository = securityContextRepository;
        this.dummyHash = passwordEncoder.encode("senha-ficticia-para-igualar-o-tempo");
    }

    @Transactional(readOnly = true)
    AuthenticatedUser login(LoginRequest credentials, HttpServletRequest request, HttpServletResponse response) {
        User user = users.findByEmail(credentials.email()).orElse(null);
        if (user == null) {
            passwordEncoder.matches(credentials.password(), dummyHash);
            throw invalidCredentials();
        }
        if (!passwordEncoder.matches(credentials.password(), user.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (user.getStatus() != UserStatus.ATIVO) {
            throw new ApiException(HttpStatus.FORBIDDEN, ACCOUNT_PENDING, "Conta pendente de ativação",
                    "Sua conta ainda não foi ativada. Use o link enviado para o seu e-mail para ativá-la.");
        }

        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getName(), user.getEmail());
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(principal, null, AUTHORITIES);
        sessionStrategy.onAuthentication(authentication, request, response);
        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return principal;
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS, "Não foi possível entrar",
                "E-mail ou senha inválidos");
    }
}
