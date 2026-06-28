package cc.hrva.ladica.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "LOGIN_TOKEN")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginToken {

    @Id
    @SequenceGenerator(name = "login_token_id_seq", allocationSize = 1)
    @GeneratedValue(generator = "login_token_id_seq", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "TOKEN_HASH", unique = true, nullable = false)
    private String tokenHash;

    @Column(name = "EXPIRES_AT", nullable = false)
    private LocalDateTime expiresAt;

    private LocalDateTime usedAt;

}
