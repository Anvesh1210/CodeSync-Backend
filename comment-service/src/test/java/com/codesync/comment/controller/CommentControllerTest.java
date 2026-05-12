package com.codesync.comment.controller;

import com.codesync.comment.entity.Comment;
import com.codesync.comment.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @Autowired
    private ObjectMapper objectMapper;

    private Comment mockComment;
    private UUID commentId;

    @BeforeEach
    void setUp() {
        commentId = UUID.randomUUID();
        mockComment = new Comment();
        mockComment.setCommentId(commentId);
        mockComment.setContent("Test comment");
    }

    @Test
    void addComment_ShouldReturnCreated() throws Exception {
        when(commentService.addComment(any(Comment.class))).thenReturn(mockComment);

        mockMvc.perform(post("/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mockComment)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").value(commentId.toString()));
    }

    @Test
    void getByFile_ShouldReturnOk() throws Exception {
        when(commentService.getByFile("file1")).thenReturn(Arrays.asList(mockComment));

        mockMvc.perform(get("/comments/file/file1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commentId").value(commentId.toString()));
    }

    @Test
    void getByProject_ShouldReturnOk() throws Exception {
        when(commentService.getByProject("proj1")).thenReturn(Arrays.asList(mockComment));

        mockMvc.perform(get("/comments/project/proj1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commentId").value(commentId.toString()));
    }

    @Test
    void getCommentById_ShouldReturnOk() throws Exception {
        when(commentService.getCommentById(commentId)).thenReturn(mockComment);

        mockMvc.perform(get("/comments/" + commentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(commentId.toString()));
    }

    @Test
    void updateComment_ShouldReturnOk() throws Exception {
        when(commentService.updateComment(eq(commentId), anyString())).thenReturn(mockComment);

        mockMvc.perform(put("/comments/" + commentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("New content"))
                .andExpect(status().isOk());
    }

    @Test
    void resolveComment_ShouldReturnOk() throws Exception {
        when(commentService.resolveComment(commentId)).thenReturn(mockComment);

        mockMvc.perform(put("/comments/" + commentId + "/resolve"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteComment_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/comments/" + commentId))
                .andExpect(status().isNoContent());
    }

    @Test
    void getCommentCount_ShouldReturnCount() throws Exception {
        when(commentService.getCommentCount("file1")).thenReturn(5L);

        mockMvc.perform(get("/comments/file/file1/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }
}
