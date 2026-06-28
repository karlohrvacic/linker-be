package cc.hrva.ladica.security;

import cc.hrva.ladica.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private static final String ROLE_USER = "ROLE_USER";

    private final UserAccountRepository userAccountRepository;

    @Override
    public UserDetails loadUserByUsername(final String email) {
        final var userAccount = userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: %s".formatted(email)));

        return new User(
                userAccount.getEmail(),
                "",
                List.of(new SimpleGrantedAuthority(ROLE_USER)));
    }

}
