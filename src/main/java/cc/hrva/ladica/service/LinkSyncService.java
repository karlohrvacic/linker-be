package cc.hrva.ladica.service;

import cc.hrva.ladica.dto.LinkDto;
import cc.hrva.ladica.dto.PullResponse;
import cc.hrva.ladica.dto.PushResponse;
import cc.hrva.ladica.model.UserAccount;

import java.util.List;

public interface LinkSyncService {
    PullResponse pull(UserAccount user, long since);
    PushResponse push(UserAccount user, List<LinkDto> links);
}
