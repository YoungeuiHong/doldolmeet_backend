package com.doldolmeet.domain.file.upload.controller;

/*
 *파일을 로컬 저장소 storage에 업로드 합니다.
 */
import com.doldolmeet.domain.file.upload.service.ChunkUploadService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@Controller
@RequiredArgsConstructor
@ApiResponse
public class ChunkUploadController {

    private final ChunkUploadService chunkUploadService;

    @GetMapping("/chunk")
    public String chunkUploadPage() {
        return "chunk";
    }

    @ResponseBody
    @GetMapping("/chunk/upload/{key}")
    public ResponseEntity<?> getLastChunkNumber(@PathVariable String key) {
        return ResponseEntity.ok(chunkUploadService.getLastChunkNumber(key));
    }

    @ResponseBody
    @PostMapping("/chunk/upload/{key}")
    public ResponseEntity<String> chunkUpload(@RequestParam("chunk") MultipartFile file,
                                              @RequestParam("chunkNumber") int chunkNumber,
                                              @RequestParam("totalChunks") int totalChunks,
                                              @PathVariable String key) throws IOException {
        boolean isDone = chunkUploadService.chunkUpload(file, chunkNumber, totalChunks, key);

        return isDone ?
                ResponseEntity.ok("File uploaded successfully") :
                ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).build();
    }
}