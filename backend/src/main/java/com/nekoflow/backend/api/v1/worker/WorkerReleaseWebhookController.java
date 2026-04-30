package com.nekoflow.backend.api.v1.worker;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nekoflow.backend.api.v1.worker.dto.WorkerReleaseResponse;

@RestController
@RequestMapping("/api/v1/worker/webhooks")
public class WorkerReleaseWebhookController {

    private final WorkerReleaseWebhookService workerReleaseWebhookService;

    public WorkerReleaseWebhookController(WorkerReleaseWebhookService workerReleaseWebhookService) {
        this.workerReleaseWebhookService = workerReleaseWebhookService;
    }

    @PostMapping("/releases")
    public ResponseEntity<WorkerReleaseResponse> publishRelease(
        @RequestBody String rawBody,
        @RequestHeader HttpHeaders headers
    ) {
        return ResponseEntity.ok(workerReleaseWebhookService.publishRelease(rawBody, headers));
    }
}
