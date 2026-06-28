package cc.hrva.ladica.dto;

import java.util.List;

public record PullResponse(List<LinkDto> links, long cursor) {
}
