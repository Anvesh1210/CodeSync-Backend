package com.codesync.comment.service;

import com.codesync.comment.entity.Comment;

import java.util.List;
import java.util.UUID;

public interface CommentService {
    Comment addComment(Comment comment);
    List<Comment> getByFile(String fileId);
    List<Comment> getByProject(String projectId);
    Comment getCommentById(UUID commentId);
    List<Comment> getReplies(UUID parentCommentId);
    Comment updateComment(UUID commentId, String content);
    void deleteComment(UUID commentId);
    Comment resolveComment(UUID commentId);
    Comment unresolveComment(UUID commentId);
    List<Comment> getByLine(Integer lineNumber);
    long getCommentCount(String fileId);
}
