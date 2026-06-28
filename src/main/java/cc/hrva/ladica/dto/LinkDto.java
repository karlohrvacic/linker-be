package cc.hrva.ladica.dto;

import cc.hrva.ladica.model.LadicaLink;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LinkDto(
        @NotBlank @Size(max = 64) String clientId,
        @NotBlank @Size(max = 2048) String url,
        @Size(max = 512) String name,
        @Size(max = 32) String cat,
        long updatedAt,
        boolean deleted) {

    public static LinkDto from(final LadicaLink link) {
        return new LinkDto(
                link.getClientId(),
                link.getUrl(),
                link.getName(),
                link.getCat(),
                link.getUpdatedAt(),
                link.isDeleted());
    }

}
