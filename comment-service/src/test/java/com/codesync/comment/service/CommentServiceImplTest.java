package com.codesync.comment.service;

import com.codesync.comment.entity.Comment;
import com.codesync.comment.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Comment mockComment;
    private UUID commentId;
    private String fileId;
    private String projectId;

    @BeforeEach
    void setUp() {
        commentId = UUID.randomUUID();
        fileId = UUID.randomUUID().toString();
        projectId = UUID.randomUUID().toString();

        mockComment = new Comment();
        mockComment.setCommentId(commentId);
        mockComment.setFileId(fileId);
        mockComment.setProjectId(projectId);
        mockComment.setContent("Test comment");
        mockComment.setLineNumber(10);
        mockComment.setResolved(false);
    }

    @Test
    void addComment_ShouldSucceed() {
        when(commentRepository.save(any(Comment.class))).thenReturn(mockComment);

        Comment result = commentService.addComment(new Comment());

        assertNotNull(result);
        assertFalse(result.getResolved());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void getByFile_ShouldReturnList() {
        when(commentRepository.findByFileId(fileId)).thenReturn(Arrays.asList(mockComment));

        List<Comment> result = commentService.getByFile(fileId);

        assertEquals(1, result.size());
        assertEquals(fileId, result.get(0).getFileId());
    }

    @Test
    void getByProject_ShouldReturnList() {
        when(commentRepository.findByProjectId(projectId)).thenReturn(Arrays.asList(mockComment));

        List<Comment> result = commentService.getByProject(projectId);

        assertEquals(1, result.size());
        assertEquals(projectId, result.get(0).getProjectId());
    }

    @Test
    void getCommentById_ShouldReturnComment_WhenExists() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(mockComment));

        Comment result = commentService.getCommentById(commentId);

        assertNotNull(result);
        assertEquals(commentId, result.getCommentId());
    }

    @Test
    void getCommentById_ShouldThrowException_WhenNotExists() {
        when(commentRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> commentService.getCommentById(UUID.randomUUID()));
    }

    @Test
    void getReplies_ShouldReturnList() {
        UUID parentId = UUID.randomUUID();
        when(commentRepository.findByParentCommentId(parentId)).thenReturn(Arrays.asList(mockComment));

        List<Comment> result = commentService.getReplies(parentId);

        assertEquals(1, result.size());
    }

    @Test
    void updateComment_ShouldSucceed() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(mockComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(mockComment);

        Comment result = commentService.updateComment(commentId, "Updated content");

        assertNotNull(result);
        assertEquals("Updated content", result.getContent());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void deleteComment_ShouldSucceed() {
        commentService.deleteComment(commentId);
        verify(commentRepository).deleteByCommentId(commentId);
    }

    @Test
    void resolveComment_ShouldSetResolvedTrue() {
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(mockComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(mockComment);

        Comment result = commentService.resolveComment(commentId);

        assertTrue(result.getResolved());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void unresolveComment_ShouldSetResolvedFalse() {
        mockComment.setResolved(true);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(mockComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(mockComment);

        Comment result = commentService.unresolveComment(commentId);

        assertFalse(result.getResolved());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void getByLine_ShouldReturnList() {
        when(commentRepository.findByLineNumber(10)).thenReturn(Arrays.asList(mockComment));

        List<Comment> result = commentService.getByLine(10);

        assertEquals(1, result.size());
    }

    @Test
    void getCommentCount_ShouldReturnCount() {
        when(commentRepository.countByFileId(fileId)).thenReturn(5L);

        long result = commentService.getCommentCount(fileId);

        assertEquals(5L, result);
    }
}
