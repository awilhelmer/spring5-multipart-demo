package com.example.demo.controller;

import com.example.demo.model.FileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.servlet.annotation.MultipartConfig;
import java.io.File;
import java.io.IOException;

/**
 * This sample just illustrate a multipart rest delegation. It's blocking, it would be nice to delegate the stream in a non blocking way.
 * The Controller received a file and delegate it to another endpoint
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
   public Mono<ResponseEntity<String>> uploadFile(@RequestPart("json") Mono<FileInfo> fileInfo, @RequestPart("file") Mono<FilePart> file) {
      LOG.info("uploadFile called ...");
      Mono<Tuple2<MultiValueMap<String, Object>, File>> map = fileInfo.zipWith(file).map(this::mapToMultiFormMap);
      Mono<Tuple2<String, File>> response = map.flatMap(multiValueMap -> webClient.post()
            .uri(uriBuilder -> uriBuilder.path("/file-upload2").build())
            .body(BodyInserters.fromMultipartData(multiValueMap.getT1()))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(String.class)
            .doAfterTerminate(() -> multiValueMap.getT2().delete())
            .map(s -> Tuples.of(s, multiValueMap.getT2())));

      return response.map(s -> new ResponseEntity<>(s.getT1(), HttpStatus.OK));
   }

   private Tuple2<MultiValueMap<String, Object>, File> mapToMultiFormMap(Tuple2<FileInfo, FilePart> objetcs) {

      File file = createTempFile(objetcs);
      objetcs.getT2().transferTo(file);
      MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
      parts.add("file", new HttpEntity<>(new FileSystemResource(file), headers));
      headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      parts.add("json", new HttpEntity<>(objetcs.getT1(), headers));
      return Tuples.of(parts, file);
   }

   private File createTempFile(Tuple2<FileInfo, FilePart> objetcs) {
      File file = null;
      try {
         file = File.createTempFile(objetcs.getT2().filename(), ".tmp");
      }
      catch (IOException e) {
         LOG.error("", e);
         throw new RuntimeException("Can't create Tempfile");
      }
      return file;
   }

   @PostMapping(value = "/file-upload2")
   public Mono<ResponseEntity<String>> upload2Test(@RequestPart("json") FileInfo fileInfo, @RequestPart("file") FilePart file) {
      LOG.info("uploadFile2 called ...");

      LOG.info(String.format("Content Type of Bytes: %s", file.headers().getContentType()));
      File newFile = new File("./" + fileInfo.getFileName());
      try {
         newFile.createNewFile();
      }
      catch (IOException e) {
         LOG.error("", e);
      }
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
