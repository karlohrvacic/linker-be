package cc.hrva.ladica.util;

import cc.hrva.ladica.exception.NotFoundException;
import cc.hrva.ladica.model.UserAccount;
import cc.hrva.ladica.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final UserAccountRepository userAccountRepository;

    public UserAccount resolve() {
        final var email = SecurityContextHolder.getContext().getAuthentication().getName();

        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
    }

}
