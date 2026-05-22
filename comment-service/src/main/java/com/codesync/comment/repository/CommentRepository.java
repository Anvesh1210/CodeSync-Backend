package com.codesync.comment.repository;

import com.codesync.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByFileId(String fileId);
    List<Comment> findByProjectId(String projectId);
    List<Comment> findByAuthorId(String authorId);
    List<Comment> findByLineNumber(Integer lineNumber);
    List<Comment> findByParentCommentId(UUID parentCommentId);
    long countByFileId(String fileId);
    List<Comment> findByResolved(Boolean resolved);
    void deleteByCommentId(UUID commentId);
}
