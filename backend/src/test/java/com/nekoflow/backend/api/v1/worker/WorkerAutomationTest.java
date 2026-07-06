package com.nekoflow.backend.api.v1.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkerAutomationTest {

    @Mock private AdminWorkerService workerService;
    @Mock private WorkerLock workerLock;

    @Test
    void disabledSchedulerDoesNothing() {
        WorkerAutomation automation = new WorkerAutomation(workerService, workerLock, false, 600);

        automation.scheduledRssPoll();

        verifyNoInteractions(workerService);
        verifyNoInteractions(workerLock);
    }

    @Test
    void enabledSchedulerPollsUnderLockAndReleases() {
        when(workerLock.tryAcquire(eq("rss-poll"), anyInt())).thenReturn(true);
        when(workerService.pollSources(any())).thenReturn(Map.of("new", 3, "sources", 2));

        WorkerAutomation automation = new WorkerAutomation(workerService, workerLock, true, 600);
        automation.scheduledRssPoll();

        verify(workerService).pollSources(any());
        verify(workerLock).release("rss-poll");
    }

    @Test
    void enabledButLockedIsSkippedWithoutPolling() {
        when(workerLock.tryAcquire(eq("rss-poll"), anyInt())).thenReturn(false);

        WorkerAutomation automation = new WorkerAutomation(workerService, workerLock, true, 600);
        automation.scheduledRssPoll();

        verify(workerService, never()).pollSources(any());
        verify(workerLock, never()).release(any());
    }

    @Test
    void releasesLockEvenWhenPollThrows() {
        when(workerLock.tryAcquire(eq("rss-poll"), anyInt())).thenReturn(true);
        when(workerService.pollSources(any())).thenThrow(new RuntimeException("boom"));

        WorkerAutomation automation = new WorkerAutomation(workerService, workerLock, true, 600);
        automation.scheduledRssPoll();

        verify(workerLock).release("rss-poll");
    }
}
