package com.codesync.website.test;

import com.codesync.website.dto.CodeFileDto;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestDeserialization {
    public static void main(String[] args) throws Exception {
        String json = "{\"projectId\": \"01294b46-b57d-4fbc-9d5b-61f6ed53a701\", \"name\": \"Hello\", \"path\": \"/\", \"language\": \"folder\", \"content\": \"\", \"isFolder\": true, \"userId\": \"bf5b4792-692d-4c5c-b405-037718a65c13\"}";
        ObjectMapper mapper = new ObjectMapper();
        CodeFileDto dto = mapper.readValue(json, CodeFileDto.class);
        System.out.println("Deserialized: " + dto);
    }
}
