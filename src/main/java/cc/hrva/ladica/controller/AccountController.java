package cc.hrva.ladica.controller;

import cc.hrva.ladica.dto.AccountDto;
import cc.hrva.ladica.repository.LadicaLinkRepository;
import cc.hrva.ladica.repository.LoginTokenRepository;
import cc.hrva.ladica.repository.UserAccountRepository;
import cc.hrva.ladica.util.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/account")
public class AccountController {

    private final CurrentUser currentUser;
    private final LadicaLinkRepository ladicaLinkRepository;
    private final LoginTokenRepository loginTokenRepository;
    private final UserAccountRepository userAccountRepository;

    @Operation(summary = "Get account", description = "Return the authenticated user's account details and current link count.")
    @GetMapping
    public AccountDto getAccount() {
        final var user = currentUser.resolve();

        return AccountDto.from(user, ladicaLinkRepository.countByUserAndDeletedFalse(user));
    }

    @Operation(summary = "Delete account", description = "Permanently erase the authenticated user and all of their links (GDPR erasure).")
    @DeleteMapping
    @Transactional
    public ResponseEntity<Void> deleteAccount() {
        final var user = currentUser.resolve();
        log.info("Erasing account {} and all of its links", user.getId());

        ladicaLinkRepository.deleteAll(ladicaLinkRepository.findByUser(user));
        loginTokenRepository.deleteByEmail(user.getEmail());
        userAccountRepository.delete(user);

        return ResponseEntity.noContent().build();
    }

}
