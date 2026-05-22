package com.codesync.comment.service;

import com.codesync.comment.entity.Comment;
import com.codesync.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;

    @Override
    public Comment addComment(Comment comment) {
        comment.setResolved(false);
        // Note: In a full implementation, this service would parse @mentions and trigger Notification-Service
        return commentRepository.save(comment);
    }

    @Override
    public List<Comment> getByFile(String fileId) {
        return commentRepository.findByFileId(fileId);
    }

    @Override
    public List<Comment> getByProject(String projectId) {
        return commentRepository.findByProjectId(projectId);
    }

    @Override
    public Comment getCommentById(UUID commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
    }

    @Override
    public List<Comment> getReplies(UUID parentCommentId) {
        return commentRepository.findByParentCommentId(parentCommentId);
    }

    @Override
    public Comment updateComment(UUID commentId, String content) {
        Comment comment = getCommentById(commentId);
        comment.setContent(content);
        return commentRepository.save(comment);
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId) {
        commentRepository.deleteByCommentId(commentId);
    }

    @Override
    public Comment resolveComment(UUID commentId) {
        Comment comment = getCommentById(commentId);
        comment.setResolved(true);
        return commentRepository.save(comment);
    }

    @Override
    public Comment unresolveComment(UUID commentId) {
        Comment comment = getCommentById(commentId);
        comment.setResolved(false);
        return commentRepository.save(comment);
    }

    @Override
    public List<Comment> getByLine(Integer lineNumber) {
        return commentRepository.findByLineNumber(lineNumber);
    }

    @Override
    public long getCommentCount(String fileId) {
        return commentRepository.countByFileId(fileId);
    }
}
