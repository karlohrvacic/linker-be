package cc.hrva.ladica.repository;

import cc.hrva.ladica.model.LadicaLink;
import cc.hrva.ladica.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LadicaLinkRepository extends JpaRepository<LadicaLink, Long> {

    List<LadicaLink> findByUserAndServerSeqGreaterThanOrderByServerSeqAsc(UserAccount user, long serverSeq);
    Optional<LadicaLink> findByUserAndClientId(UserAccount user, String clientId);
    Optional<LadicaLink> findByUserAndNormalizedUrlAndDeletedFalse(UserAccount user, String normalizedUrl);
    Optional<LadicaLink> findTopByUserOrderByServerSeqDesc(UserAccount user);
    List<LadicaLink> findByUser(UserAccount user);
    long countByUserAndDeletedFalse(UserAccount user);

}
