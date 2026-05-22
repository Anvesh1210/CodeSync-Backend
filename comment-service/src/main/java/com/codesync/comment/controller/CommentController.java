package com.codesync.comment.controller;

import com.codesync.comment.entity.Comment;
import com.codesync.comment.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@Tag(name = "Comment Resource", description = "Endpoints for managing code review comments")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "Add a new comment")
    public ResponseEntity<Comment> addComment(@RequestBody Comment comment) {
        return new ResponseEntity<>(commentService.addComment(comment), HttpStatus.CREATED);
    }

    @GetMapping("/file/{fileId}")
    @Operation(summary = "Get comments by file ID")
    public ResponseEntity<List<Comment>> getByFile(@PathVariable String fileId) {
        return ResponseEntity.ok(commentService.getByFile(fileId));
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get comments by project ID")
    public ResponseEntity<List<Comment>> getByProject(@PathVariable String projectId) {
        return ResponseEntity.ok(commentService.getByProject(projectId));
    }

    @GetMapping("/{commentId}")
    @Operation(summary = "Get a comment by ID")
    public ResponseEntity<Comment> getCommentById(@PathVariable UUID commentId) {
        return ResponseEntity.ok(commentService.getCommentById(commentId));
    }

    @GetMapping("/line/{lineNumber}")
    @Operation(summary = "Get comments by line number")
    public ResponseEntity<List<Comment>> getByLine(@PathVariable Integer lineNumber) {
        return ResponseEntity.ok(commentService.getByLine(lineNumber));
    }

    @GetMapping("/{commentId}/replies")
    @Operation(summary = "Get replies to a specific comment")
    public ResponseEntity<List<Comment>> getReplies(@PathVariable UUID commentId) {
        return ResponseEntity.ok(commentService.getReplies(commentId));
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "Update comment content")
    public ResponseEntity<Comment> updateComment(@PathVariable UUID commentId, @RequestBody String content) {
        return ResponseEntity.ok(commentService.updateComment(commentId, content));
    }

    @PutMapping("/{commentId}/resolve")
    @Operation(summary = "Resolve a comment")
    public ResponseEntity<Comment> resolveComment(@PathVariable UUID commentId) {
        return ResponseEntity.ok(commentService.resolveComment(commentId));
    }

    @PutMapping("/{commentId}/unresolve")
    @Operation(summary = "Unresolve a comment")
    public ResponseEntity<Comment> unresolveComment(@PathVariable UUID commentId) {
        return ResponseEntity.ok(commentService.unresolveComment(commentId));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Delete a comment")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/file/{fileId}/count")
    @Operation(summary = "Get total comment count for a file")
    public ResponseEntity<Long> getCommentCount(@PathVariable String fileId) {
        return ResponseEntity.ok(commentService.getCommentCount(fileId));
    }
}
