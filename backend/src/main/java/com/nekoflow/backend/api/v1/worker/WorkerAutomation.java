package com.nekoflow.backend.api.v1.worker;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Worker automatico (B2): dispara o poll RSS periodicamente.
 *
 * DESLIGADO por padrao (app.worker.rss-poll.enabled=false) — so caca sozinho
 * quando explicitamente habilitado. Usa WorkerLock (B3) para nao rodar em
 * duplicidade entre agendamentos concorrentes ou multiplas instancias.
 */
@Component
public class WorkerAutomation {

    private static final Logger log = LoggerFactory.getLogger(WorkerAutomation.class);
    private static final String LOCK_NAME = "rss-poll";

    private final AdminWorkerService workerService;
    private final WorkerLock workerLock;
    private final boolean enabled;
    private final int leaseSeconds;

    public WorkerAutomation(
        AdminWorkerService workerService,
        WorkerLock workerLock,
        @Value("${app.worker.rss-poll.enabled:false}") boolean enabled,
        @Value("${app.worker.rss-poll.lease-seconds:600}") int leaseSeconds
    ) {
        this.workerService = workerService;
        this.workerLock = workerLock;
        this.enabled = enabled;
        this.leaseSeconds = leaseSeconds;
    }

    @Scheduled(
        fixedDelayString = "${app.worker.rss-poll.interval-ms:1800000}",
        initialDelayString = "${app.worker.rss-poll.initial-delay-ms:60000}"
    )
    public void scheduledRssPoll() {
        if (!enabled) {
            return;
        }
        if (!workerLock.tryAcquire(LOCK_NAME, leaseSeconds)) {
            log.info("RSS poll automatico pulado: lock '{}' ja esta em uso.", LOCK_NAME);
            return;
        }
        try {
            Map<String, Object> result = workerService.pollSources(Map.of());
            log.info("RSS poll automatico: {} novas releases de {} fontes.", result.get("new"), result.get("sources"));
        } catch (Exception exception) {
            log.warn("RSS poll automatico falhou: {}", exception.getMessage());
        } finally {
            workerLock.release(LOCK_NAME);
        }
    }
}
