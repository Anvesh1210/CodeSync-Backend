package com.codesync.file.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class FileTreeNodeTest {
    private String name;
    @JsonProperty("isFolder")
    private boolean isFolder;
    @Builder.Default
    private List<FileTreeNodeTest> children = new ArrayList<>();
}

public class JacksonTest {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        
        FileTreeNodeTest file = FileTreeNodeTest.builder().name("file.txt").isFolder(false).build();
        FileTreeNodeTest folder = FileTreeNodeTest.builder().name("src").isFolder(true).build();
        folder.getChildren().add(file);
        
        System.out.println(mapper.writeValueAsString(folder));
    }
}
