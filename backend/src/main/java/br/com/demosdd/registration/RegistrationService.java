package br.com.demosdd.registration;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.demosdd.shared.web.ApiException;
import br.com.demosdd.shared.web.FieldErrorResponse;
import br.com.demosdd.user.Address;
import br.com.demosdd.user.User;
import br.com.demosdd.user.UserRepository;
import br.com.demosdd.user.UserStatus;

/**
 * Cadastro de usuários (RF03) com a regra de unicidade (RN03).
 * Tudo acontece em uma transação: gravação, publicação de {@link UserRegistered}
 * e envio do e-mail de ativação pelo listener síncrono.
 */
@Service
public class RegistrationService {

    static final String ALREADY_REGISTERED = "ALREADY_REGISTERED";
    static final String PENDING_ACTIVATION = "PENDING_ACTIVATION";

    private final UserRepository users;
    private final ActivationLinkChecker activationLinks;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    RegistrationService(UserRepository users, ActivationLinkChecker activationLinks,
                        PasswordEncoder passwordEncoder, ApplicationEventPublisher events, Clock clock) {
        this.users = users;
        this.activationLinks = activationLinks;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.clock = clock;
    }

    /** Recebe dados já normalizados e validados (ver {@link RegistrationRequest}). */
    @Transactional
    public User register(RegistrationRequest request) {
        Instant now = clock.instant();
        resolveConflicts(request, now);

        User user = User.pending(request.name(), request.cpf(), request.email(), request.birthDate(),
                passwordEncoder.encode(request.password()), request.phone(),
                new Address(request.cep(), request.street(), request.number(), request.complement(),
                        request.district(), request.city(), request.state()),
                now);
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // Cadastro simultâneo com o mesmo e-mail/CPF: a constraint do banco decide.
            throw alreadyRegistered(fieldFromConstraint(ex));
        }

        events.publishEvent(new UserRegistered(user.getId(), user.getEmail(), user.getName()));
        return user;
    }

    /**
     * RN03: recusa se o e-mail ou o CPF pertencem a uma conta ativa ou a um
     * cadastro pendente com link ainda válido. Se todos os conflitos forem
     * pendentes com link expirado, eles são removidos para dar lugar ao novo cadastro.
     */
    private void resolveConflicts(RegistrationRequest request, Instant now) {
        List<User> conflicts = users.findByEmailOrCpf(request.email(), request.cpf());
        if (conflicts.isEmpty()) {
            return;
        }

        List<User> blocking = conflicts.stream()
                .filter(user -> user.getStatus() == UserStatus.ATIVO
                        || activationLinks.hasValidActivationLink(user.getId(), now))
                .toList();

        if (blocking.isEmpty()) {
            users.deleteAll(conflicts);
            users.flush();
            return;
        }

        List<FieldErrorResponse> errors = new ArrayList<>();
        for (User user : blocking) {
            boolean active = user.getStatus() == UserStatus.ATIVO;
            if (user.getEmail().equals(request.email())) {
                errors.add(new FieldErrorResponse("email", active
                        ? "E-mail já cadastrado"
                        : "Este e-mail possui um cadastro aguardando ativação"));
            }
            if (user.getCpf().equals(request.cpf())) {
                errors.add(new FieldErrorResponse("cpf", active
                        ? "CPF já cadastrado"
                        : "Este CPF possui um cadastro aguardando ativação"));
            }
        }

        boolean onlyPending = blocking.stream().allMatch(user -> user.getStatus() == UserStatus.PENDENTE);
        if (onlyPending) {
            throw new ApiException(HttpStatus.CONFLICT, PENDING_ACTIVATION, "Cadastro aguardando ativação",
                    "Já existe um cadastro aguardando ativação. Verifique seu e-mail para ativar a conta.",
                    errors);
        }
        throw new ApiException(HttpStatus.CONFLICT, ALREADY_REGISTERED, "Dados já cadastrados",
                "Já existe uma conta com estes dados.", errors);
    }

    private static ApiException alreadyRegistered(String field) {
        String message = "cpf".equals(field) ? "CPF já cadastrado" : "E-mail já cadastrado";
        return new ApiException(HttpStatus.CONFLICT, ALREADY_REGISTERED, "Dados já cadastrados",
                "Já existe uma conta com estes dados.", List.of(new FieldErrorResponse(field, message)));
    }

    private static String fieldFromConstraint(DataIntegrityViolationException ex) {
        String message = String.valueOf(ex.getMostSpecificCause().getMessage());
        return message.contains("uk_users_cpf") ? "cpf" : "email";
    }
}
