package com.nekoflow.backend.api.v1.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nekoflow.backend.api.v1.admin.dto.AdminSeekStreamingFolderResponse;
import com.nekoflow.backend.api.v1.admin.dto.AdminSeekStreamingVideoResponse;

@RestController
@Validated
@RequestMapping("/api/v1/admin/seekstreaming")
public class AdminSeekStreamingController {

    private final SeekStreamingService seekStreamingService;

    public AdminSeekStreamingController(SeekStreamingService seekStreamingService) {
        this.seekStreamingService = seekStreamingService;
    }

    @GetMapping("/folders")
    public ResponseEntity<List<AdminSeekStreamingFolderResponse>> listFolders() {
        return ResponseEntity.ok(seekStreamingService.listFolders());
    }

    @GetMapping("/folders/{folderId}/videos")
    public ResponseEntity<List<AdminSeekStreamingVideoResponse>> listVideos(@PathVariable String folderId) {
        return ResponseEntity.ok(seekStreamingService.listVideos(folderId));
    }
}
