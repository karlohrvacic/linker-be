package cc.hrva.ladica.service;

public interface MagicLinkService {
    void requestLink(String email);
    String verify(String rawToken);
}
