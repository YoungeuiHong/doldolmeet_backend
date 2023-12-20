package com.doldolmeet.papago;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PapagoController {

    @Autowired
    private PapagoService papagoService;

    @PostMapping("/translate")
    public ResponseEntity<Map<String, String>> translate(@RequestParam String target, @RequestBody Map<String, String> requestBody) throws JsonProcessingException {
        String inputText = requestBody.get("text");
        return papagoService.translateText(target, inputText);
    }
}
