package com.example.demo.controller;

import com.example.demo.model.FileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.servlet.annotation.MultipartConfig;
import java.io.File;

/**
 * This sample just illustrate a multipart rest delegation. It's blocking, it would be nice to delegate the stream in a non blocking way.
 * The Controller received a file and delegate it to another endpoint
 * (here itself, seems to be not working, but the error "No suitable writer found for part: file" comes up)
 * In a real application the other endpoint (file-upload2) is running on another service
 *
 * @author Alexander Wilhelmer
 */
@MultipartConfig(fileSizeThreshold = 20971520)
@RestController
@RequestMapping("/test")
public class FileController {

   private static final Logger LOG = LoggerFactory.getLogger(FileController.class);

   private WebClient webClient = WebClient.builder().baseUrl("http://localhost:8080/test").build();

   @PostMapping(value = "/file-upload")
   public Mono<ResponseEntity<String>> uploadFile(@RequestPart("json") FileInfo fileInfo, @RequestPart("file") FilePart file) {
      LOG.info("uploadFile called ...");
      LOG.info(String.format("Content Type of File: %s", file.headers().getContentType()));

      MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      parts.add("file", new HttpEntity<>(file, headers));
      headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      parts.add("json", new HttpEntity<>(fileInfo, headers));

      Mono<String> response = webClient.post()
            .uri(uriBuilder -> uriBuilder.path("/file-upload2").build())
            .body(BodyInserters.fromMultipartData(parts))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(String.class);

      return response.map(s -> new ResponseEntity<>(s, HttpStatus.OK));
   }

   @PostMapping(value = "/file-upload2")
   public Mono<ResponseEntity<String>> upload2Test(@RequestPart("json") FileInfo fileInfo, @RequestPart("file") FilePart file) {
      LOG.info("uploadFile2 called ...");

      LOG.info(String.format("Content Type of Bytes: %s", file.headers().getContentType()));
      File newFile = new File("./" + fileInfo.getFileName());
      file.transferTo(newFile);
      LOG.info(String.format("Size of new File: %s", newFile.length()));

      return Mono.just(new ResponseEntity<>("File written!", HttpStatus.OK));
   }

   @ExceptionHandler
   public Mono<ResponseEntity<?>> handleException(Exception e) {
      LOG.error("Error!", e);
      return Mono.just(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
   }
}
