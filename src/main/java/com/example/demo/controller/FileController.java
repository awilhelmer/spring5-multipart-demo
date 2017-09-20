package com.example.demo.controller;

import com.example.demo.model.FileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;

/**
 * This sample just illustrate a multipart rest delegation. It's blocking, it would be nice to delegate the stream in a non blocking way.
 * The Controller received a file and delegate it to another endpoint
 * In a real application the other endpoint (file-upload2) is running on another service
 *
 * @author Alexander Wilhelmer
 */
@RestController
@RequestMapping("/test")
public class FileController {

   private static final Logger LOG = LoggerFactory.getLogger(FileController.class);

   private WebClient webClient = WebClient.builder().baseUrl("http://localhost:8080/test").build();
   private WebClient webClientHttpbin = WebClient.builder().baseUrl("http://posttestserver.com").build();

   @PostMapping(value = "/file-upload")
   public Mono<ResponseEntity<String>> uploadFile(@RequestPart("json") Mono<FileInfo> fileInfo, @RequestPart("file") Mono<FilePart> file) {
      LOG.info("uploadFile called ...");
      Mono<File> tmpFile = fileInfo.map(info -> createTempFile(info.getFileName()));
      return tmpFile.zipWith(file).map(objects -> {
         // TransferTo is blocking anyway - just returning a empty Mono...
         objects.getT2().transferTo(objects.getT1()).block();
         if (objects.getT1().length() == 0) {
            throw new RuntimeException("Zero byte File!");
         }
         return new ResponseEntity<>(HttpStatus.OK);
      });

   }

   private File createTempFile(String fileName) {
      File file = null;
      try {
         file = File.createTempFile(fileName, ".tmp");
      }
      catch (IOException e) {
         LOG.error("", e);
         throw new RuntimeException("Can't create Tempfile");
      }
      return file;
   }

   @ExceptionHandler
   public Mono<ResponseEntity<?>> handleException(Exception e) {
      LOG.error("Error!", e);
      return Mono.just(new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
   }
}
