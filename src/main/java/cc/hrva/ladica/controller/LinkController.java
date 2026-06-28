package cc.hrva.ladica.controller;

import cc.hrva.ladica.dto.PullResponse;
import cc.hrva.ladica.dto.PushRequest;
import cc.hrva.ladica.dto.PushResponse;
import cc.hrva.ladica.service.LinkSyncService;
import cc.hrva.ladica.util.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/links")
public class LinkController {

    private final LinkSyncService linkSyncService;
    private final CurrentUser currentUser;

    @Operation(summary = "Pull link changes", description = "Return the authenticated user's links changed after the given cursor, with the next cursor.")
    @GetMapping
    public PullResponse pull(@RequestParam(defaultValue = "0") final long since) {
        return linkSyncService.pull(currentUser.resolve(), since);
    }

    @Operation(summary = "Push link changes", description = "Apply last-write-wins link changes for the authenticated user and return the advanced cursor.")
    @PostMapping
    public PushResponse push(@Valid @RequestBody final PushRequest request) {
        log.info("Pushing {} link changes", request.links().size());

        return linkSyncService.push(currentUser.resolve(), request.links());
    }

}
