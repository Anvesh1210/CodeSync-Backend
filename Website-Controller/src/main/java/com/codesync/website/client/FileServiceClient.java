package com.codesync.website.client;

import com.codesync.website.dto.CodeFileDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "file-service")
public interface FileServiceClient {

    @PostMapping("/files")
    CodeFileDto createFile(@RequestBody CodeFileDto file);

    @PostMapping("/files/folder")
    CodeFileDto createFolder(@RequestBody CodeFileDto file);

    @DeleteMapping("/files/{id}")
    Object deleteFile(@PathVariable("id") String id, @RequestParam("userId") String userId);

    @PutMapping("/files/{id}/content")
    CodeFileDto updateFileContent(@PathVariable("id") String id, @RequestBody com.codesync.website.dto.FileContentRequest request);

    @PutMapping("/files/{id}/rename")
    CodeFileDto renameFile(@PathVariable("id") String id, @RequestParam("newName") String newName, @RequestParam("userId") String userId);

    @GetMapping("/files/tree/{projectId}")
    Object getFileTree(@PathVariable("projectId") String projectId);

    @GetMapping("/files/{id}")
    CodeFileDto getFileById(@PathVariable("id") String id);
}
