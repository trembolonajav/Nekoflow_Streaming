package com.nekoflow.backend.api.v1.worker;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lock leve baseado em lease (tabela worker_lock). Uma unica UPDATE atomica
 * decide quem adquire: quem consegue setar locked_until no futuro. O lease
 * expira sozinho, entao um processo que morre no meio nao trava o lock.
 * Sem dependencia nova; funciona no monolito de VM unica (e sobrevive a
 * multiplas instancias no futuro).
 */
@Component
public class WorkerLock {

    private final JdbcTemplate jdbc;

    public WorkerLock(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Tenta adquirir o lock por leaseSeconds. true se conseguiu, false se ja esta preso. */
    public boolean tryAcquire(String name, int leaseSeconds) {
        return jdbc.update(
            "update worker_lock set locked_until = now() + make_interval(secs => ?), updated_at = now() "
                + "where name = ? and locked_until < now()",
            leaseSeconds, name
        ) == 1;
    }

    /** Libera o lock imediatamente (expira o lease). */
    public void release(String name) {
        jdbc.update("update worker_lock set locked_until = now(), updated_at = now() where name = ?", name);
    }
}
