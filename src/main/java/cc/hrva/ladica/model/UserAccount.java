package cc.hrva.ladica.model;

import cc.hrva.ladica.model.enums.AuthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "USER_ACCOUNT")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount {

    @Id
    @SequenceGenerator(name = "user_account_id_seq", allocationSize = 1)
    @GeneratedValue(generator = "user_account_id_seq", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;

    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    private boolean active;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }

}
