package com.example.demo.controller;

import com.example.demo.model.FileInfo;
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

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Alexander Wilhelmer
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class FileControllerTest {
   private static final Logger LOG = LoggerFactory.getLogger(FileControllerTest.class);
   Throwable  errors = null;

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

      int threads = 20;
      int iterations = 100;
      List<Thread> threadItems = Collections.synchronizedList(new ArrayList<>());
      for (int i = 0; i < threads; i++) {
         // Sorry dude @ http://www.posttestserver.com/ ... but i need a service ...
         threadItems.add(new Thread(() -> {
            for (int j = 0; j < iterations; j++) {
               try {
                  webClient.post()
                        .uri(uriBuilder -> uriBuilder.path("/test/file-upload").build())
                        .accept(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(parts))
                        .exchange()
                        .expectStatus()
                        .is2xxSuccessful()
                        .expectBody(String.class);

               }
               catch (Throwable t) {
                  errors = t;
               }

            }
            LOG.info("Thread stopped!");
            threadItems.remove(threadItems.size() - 1);
         }));
      }
      threadItems.forEach(Thread::start);

      while (!threadItems.isEmpty() && errors == null) {
         Thread.sleep(Duration.ofSeconds(1).toMillis());
      }
      if (errors != null) {
         throw new Exception(errors);
      }
   }

}

