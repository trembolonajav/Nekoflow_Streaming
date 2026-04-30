package com.nekoflow.backend.api.v1.worker;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/worker")
public class AdminWorkerController {

    private final AdminWorkerService service;

    public AdminWorkerController(AdminWorkerService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(service.dashboard());
    }

    @PostMapping("/seek/ping")
    public ResponseEntity<Map<String, Object>> pingSeek() {
        return ResponseEntity.ok(service.pingSeek());
    }

    @PostMapping("/seek/sync")
    public ResponseEntity<Map<String, Object>> syncSeek() {
        return ResponseEntity.ok(service.syncSeek());
    }

    @GetMapping("/seek/videos")
    public ResponseEntity<Map<String, Object>> seekVideos(
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(service.seekVideos(search, page, size));
    }

    @GetMapping("/import/options")
    public ResponseEntity<Map<String, Object>> importOptions(
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "missing") String status
    ) {
        return ResponseEntity.ok(service.importOptions(search, status));
    }

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importSelected(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.importSelected(body));
    }

    @GetMapping("/queue")
    public ResponseEntity<Map<String, Object>> queue(
        @RequestParam(defaultValue = "all") String status,
        @RequestParam(defaultValue = "") String search,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "30") int size
    ) {
        return ResponseEntity.ok(service.queue(status, search, page, size));
    }

    @PostMapping("/queue/parse")
    public ResponseEntity<Map<String, Object>> parseQueue(@RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(service.parseQueue(body == null ? Map.of() : body));
    }

    @PostMapping("/queue/{id}/approve")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable String id) {
        return ResponseEntity.ok(service.approve(id));
    }

    @PostMapping("/queue/approve")
    public ResponseEntity<Map<String, Object>> approveMany(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.approveMany(body));
    }

    @PutMapping("/queue/{id}")
    public ResponseEntity<Map<String, Object>> updateQueueItem(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.updateQueueItem(id, body));
    }

    @PostMapping("/queue/{id}/publish")
    public ResponseEntity<Map<String, Object>> publish(@PathVariable String id) {
        return ResponseEntity.ok(service.publish(List.of(id)));
    }

    @PostMapping("/queue/publish")
    public ResponseEntity<Map<String, Object>> publishMany(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.publishMany(body));
    }

    @GetMapping("/logs")
    public ResponseEntity<Map<String, Object>> logs() {
        return ResponseEntity.ok(service.logs());
    }

    @GetMapping("/sources")
    public ResponseEntity<List<Map<String, Object>>> sources() {
        return ResponseEntity.ok(service.sources());
    }

    @PostMapping("/sources")
    public ResponseEntity<Map<String, Object>> createSource(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.createSource(body));
    }

    @PutMapping("/sources/{id}")
    public ResponseEntity<Map<String, Object>> updateSource(@PathVariable String id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.updateSource(id, body));
    }

    @DeleteMapping("/sources/{id}")
    public ResponseEntity<Map<String, Object>> deleteSource(@PathVariable String id) {
        service.deleteSource(id);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PostMapping("/sources/poll")
    public ResponseEntity<Map<String, Object>> pollSources(@RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(service.pollSources(body == null ? Map.of() : body));
    }
}
