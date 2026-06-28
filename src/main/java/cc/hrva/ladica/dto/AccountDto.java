package cc.hrva.ladica.dto;

import cc.hrva.ladica.model.UserAccount;
import cc.hrva.ladica.model.enums.AuthProvider;

public record AccountDto(String email, AuthProvider authProvider, long linkCount) {

    public static AccountDto from(final UserAccount user, final long linkCount) {
        return new AccountDto(user.getEmail(), user.getAuthProvider(), linkCount);
    }

}
