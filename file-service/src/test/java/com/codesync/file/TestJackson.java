package com.codesync.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class FileTreeNodeTest {
    private String name;
    private Boolean isFolder;
    @Builder.Default
    private List<FileTreeNodeTest> children = new ArrayList<>();
}

public class TestJackson {
    public static void main(String[] args) throws Exception {
        FileTreeNodeTest file = FileTreeNodeTest.builder().name("file.txt").isFolder(false).build();
        FileTreeNodeTest folder = FileTreeNodeTest.builder().name("src").isFolder(true).build();
        folder.getChildren().add(file);

        ObjectMapper mapper = new ObjectMapper();
        System.out.println(mapper.writeValueAsString(folder));
    }
}
