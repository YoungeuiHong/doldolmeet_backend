package com.doldolmeet.papago;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Service
@Getter
@RequiredArgsConstructor
public class PapagoService {

    @Value("${naver.papago.clientId}")
    private String clientId;

    @Value("${naver.papago.clientSecret}")
    private String clientSecret;

    private final String apiURL1 = "https://openapi.naver.com/v1/papago/n2mt";
    private final String apiURL2 = "https://openapi.naver.com/v1/papago/detectLangs";
    private final ObjectMapper objectMapper;

    public ResponseEntity<Map<String, String>> translateText(String target, String inputText) throws JsonProcessingException {
        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("X-Naver-Client-Id", clientId);
        requestHeaders.put("X-Naver-Client-Secret", clientSecret);

        String text;
        try {
            text = URLEncoder.encode(inputText, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("인코딩 실패", e);
        }

        String detectedLanguage = postDetect(apiURL2, requestHeaders, text);
        System.out.println("Detected Language: " + detectedLanguage);

        String rawtranslatedText = post(apiURL1, requestHeaders, text, detectedLanguage, target);
        System.out.println("Translated Text: " + rawtranslatedText);

        Map<String, String> result = new HashMap<>();
        // JSON 문자열을 JsonNode로 파싱
        JsonNode jsonNode = objectMapper.readTree(rawtranslatedText);

        // 필요한 값 추출
        String srcLangType = jsonNode.get("message").get("result").get("srcLangType").asText();
        String tarLangType = jsonNode.get("message").get("result").get("tarLangType").asText();
        String translatedText = jsonNode.get("message").get("result").get("translatedText").asText();

        // 추출한 값 출력
        System.out.println("Source Language Type: " + srcLangType);
        System.out.println("Target Language Type: " + tarLangType);
        System.out.println("Translated Text: " + translatedText);
        result.put("srcLangType", srcLangType);
        result.put("tarLangType", tarLangType);
        result.put("translatedText", translatedText);

        return ResponseEntity.ok(result);
    }

    private String post(String apiUrl, Map<String, String> requestHeaders, String text, String sourceLanguage, String target) {
        HttpURLConnection con = connect(apiUrl);
        String postParams = "source=" + sourceLanguage + "&target=" + target + "&text=" + text;

        try {
            con.setRequestMethod("POST");
            for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }

            con.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                wr.write(postParams.getBytes());
                wr.flush();
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return readBody(con.getInputStream());
            } else {
                return readBody(con.getErrorStream());
            }
        } catch (IOException e) {
            throw new RuntimeException("API 요청과 응답 실패", e);
        } finally {
            con.disconnect();
        }
    }

    private String postDetect(String apiUrl, Map<String, String> requestHeaders, String text) {
        HttpURLConnection con = connect(apiUrl);
        String postParams = "query=" + text;
        try {
            con.setRequestMethod("POST");
            for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }

            con.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                wr.write(postParams.getBytes());
                wr.flush();
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String responseBody = readBody(con.getInputStream());
                JSONObject json = new JSONObject(responseBody);
                return json.getString("langCode");
            } else {
                return readBody(con.getErrorStream());
            }
        } catch (IOException e) {
            throw new RuntimeException("API 요청과 응답 실패", e);
        } finally {
            con.disconnect();
        }
    }

    private String readBody(InputStream body) {
        InputStreamReader streamReader = new InputStreamReader(body);
        try (BufferedReader lineReader = new BufferedReader(streamReader)) {
            StringBuilder responseBody = new StringBuilder();
            String line;
            while ((line = lineReader.readLine()) != null) {
                responseBody.append(line);
            }
            return responseBody.toString();
        } catch (IOException e) {
            throw new RuntimeException("API 응답을 읽는데 실패했습니다.", e);
        }
    }

    private HttpURLConnection connect(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            return (HttpURLConnection) url.openConnection();
        } catch (MalformedURLException e) {
            throw new RuntimeException("API URL이 잘못되었습니다. : " + apiUrl, e);
        } catch (IOException e) {
            throw new RuntimeException("연결이 실패했습니다. : " + apiUrl, e);
        }
    }
}