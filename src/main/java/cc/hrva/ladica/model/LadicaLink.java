package cc.hrva.ladica.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "LADICA_LINK",
       uniqueConstraints = @UniqueConstraint(columnNames = {"USER_ID", "CLIENT_ID"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LadicaLink {

    @Id
    @SequenceGenerator(name = "ladica_link_id_seq", allocationSize = 1)
    @GeneratedValue(generator = "ladica_link_id_seq", strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(optional = false)
    private UserAccount user;

    @Column(name = "CLIENT_ID", nullable = false)
    private String clientId;

    @Column(nullable = false)
    private String url;

    @Column(name = "NORMALIZED_URL", nullable = false)
    private String normalizedUrl;

    private String name;

    private String cat;

    private long updatedAt;

    private boolean deleted;

    @Column(name = "SERVER_SEQ", nullable = false)
    private long serverSeq;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}
