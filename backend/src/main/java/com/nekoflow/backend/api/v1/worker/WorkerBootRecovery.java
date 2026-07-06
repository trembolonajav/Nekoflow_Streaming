package com.nekoflow.backend.api.v1.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Retomada no boot (B4): o drain do crawl roda num executor EM MEMORIA, entao um
 * restart no meio deixa crawl_jobs presos em 'running'/'ingesting'. O snapshot dos
 * itens restantes so existia em memoria (nao persistido), entao nao da para
 * retomar exatamente de onde parou; o seguro e marcar como 'failed' com motivo
 * claro para que o admin possa re-executar (o dedup via seen_releases evita
 * reprocessar o que ja foi ingerido).
 *
 * Tambem libera locks de worker que ficaram presos por um processo que morreu.
 */
@Component
public class WorkerBootRecovery implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkerBootRecovery.class);

    private final JdbcTemplate jdbc;

    public WorkerBootRecovery(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        int orphans = jdbc.update("""
            update crawl_jobs
            set status = 'failed',
                status_reason = trim(both ' |' from coalesce(status_reason, '') || ' | interrompido por restart (re-executar)'),
                finished_at = now(), updated_at = now()
            where status in ('running', 'ingesting')
            """);
        if (orphans > 0) {
            log.warn("Boot recovery: {} crawl_jobs orfaos marcados como 'failed' (re-executaveis).", orphans);
        }

        int released = jdbc.update("update worker_lock set locked_until = now(), updated_at = now() where locked_until > now()");
        if (released > 0) {
            log.warn("Boot recovery: {} worker_lock(s) presos foram liberados.", released);
        }
    }
}
