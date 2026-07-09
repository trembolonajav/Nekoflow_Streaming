package com.nekoflow.backend.api.v1.calendar;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.nekoflow.backend.api.v1.worker.WorkerLock;

/**
 * Sincronizacao automatica do calendario com o AniList.
 *
 * Roda a cada 6h por padrao (o cronograma so muda quando um episodio vai ao ar
 * ou a grade e ajustada). Usa WorkerLock para nao rodar em duplicidade.
 */
@Component
public class CalendarSyncAutomation {

    private static final Logger log = LoggerFactory.getLogger(CalendarSyncAutomation.class);
    private static final String LOCK_NAME = "calendar-sync";

    private final CalendarSyncService calendarSyncService;
    private final WorkerLock workerLock;
    private final boolean enabled;
    private final int leaseSeconds;

    public CalendarSyncAutomation(
        CalendarSyncService calendarSyncService,
        WorkerLock workerLock,
        @Value("${app.worker.calendar-sync.enabled:true}") boolean enabled,
        @Value("${app.worker.calendar-sync.lease-seconds:300}") int leaseSeconds
    ) {
        this.calendarSyncService = calendarSyncService;
        this.workerLock = workerLock;
        this.enabled = enabled;
        this.leaseSeconds = leaseSeconds;
    }

    @Scheduled(
        fixedDelayString = "${app.worker.calendar-sync.interval-ms:21600000}",
        initialDelayString = "${app.worker.calendar-sync.initial-delay-ms:120000}"
    )
    public void scheduledCalendarSync() {
        if (!enabled) {
            return;
        }
        if (!workerLock.tryAcquire(LOCK_NAME, leaseSeconds)) {
            log.info("Calendar sync automatico pulado: lock '{}' ja esta em uso.", LOCK_NAME);
            return;
        }
        try {
            Map<String, Object> result = calendarSyncService.sync();
            log.info("Calendar sync automatico: {} animes, {} novos agendados, {} reagendados.",
                result.get("animes"), result.get("scheduled_created"), result.get("scheduled_updated"));
        } catch (Exception exception) {
            log.warn("Calendar sync automatico falhou: {}", exception.getMessage());
        } finally {
            workerLock.release(LOCK_NAME);
        }
    }
}
