package com.example.demo.controller;

import com.example.demo.model.FileInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ResourceUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;

/**
 * @author Alexander Wilhelmer
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class FileControllerTest {
   private static final Logger LOG = LoggerFactory.getLogger(FileControllerTest.class);

   @Autowired
   private WebTestClient webClient;

   @Test
   public void uploadFile() throws Exception {
      FileInfo fileInfo = new FileInfo();
      fileInfo.setFileName("test-file.jpg");
      fileInfo.setMimeType(MediaType.IMAGE_JPEG_VALUE);

      File file = ResourceUtils.getFile(this.getClass().getResource("/test-file.jpg"));
      MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      parts.add("json", new HttpEntity<>(fileInfo, headers));
      headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      parts.add("file", new HttpEntity<>(new FileSystemResource(file), headers));

      int iterations = 1000;
      for (int i = 0; i < iterations; i++) {
         webClient.post()
               .uri(uriBuilder -> uriBuilder.path("/test/file-upload").build())
               .accept(MediaType.MULTIPART_FORM_DATA)
               .body(BodyInserters.fromMultipartData(parts))
               .exchange()
               .expectStatus()
               .is2xxSuccessful()
               .expectBody(String.class);

      }

   }

}