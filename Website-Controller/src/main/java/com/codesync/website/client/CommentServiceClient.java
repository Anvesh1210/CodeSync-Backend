package com.codesync.website.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "comment-service")
public interface CommentServiceClient {

    @GetMapping("/api/comments/project/{projectId}")
    Object getCommentsByProject(@PathVariable("projectId") String projectId);

    @PostMapping("/api/comments")
    Object addComment(@RequestBody Object comment);

    @PutMapping("/api/comments/{id}/resolve")
    Object resolveComment(@PathVariable("id") String id);
}
