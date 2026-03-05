package com.example.backend.fss.service;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * [금융감독원 API 서비스 (FSS Service)]
 * - 금감원 공시 OpenAPI를 호출하여 데이터를 가져오는 비즈니스 로직
 *
 * [application-secrets.yml에 설정]
 * fss: 
 * api-key: MY_FSS_API_KEY
 * base-url: http://fss.or.kr/fss/kr/openApi/api/v1
 *
 * [필요한 주입 객체 및 변수]
 * 1. RestTemplate restTemplate : 외부 HTTP API 호출용
 * - @Bean으로 등록하거나 new RestTemplate()으로 사용
 *
 * 2. @Value("${fss.api-key}") String fssApiKey : 금감원 API 인증 키
 *
 * 3. @Value("${fss.base-url}") String fssBaseUrl : 금감원 API 기본 URL
 *
 * [구현해야 할 메서드]
 *
 * 1. fetchFssData(String startDate, String endDate) → List<FssResponseDto> 또는
 * List<Map>
 * - 금감원 API URL 조립:
 * String url = fssBaseUrl + "/disclosure?apiType=json"
 * + "&startDate=" + startDate
 * + "&endDate=" + endDate
 * + "&apiKey=" + fssApiKey;
 *
 * - RestTemplate으로 GET 요청:
 * ResponseEntity<String> response = restTemplate.getForEntity(url,
 * String.class);
 *
 * - JSON 응답 파싱:
 * ObjectMapper mapper = new ObjectMapper();
 * JsonNode root = mapper.readTree(response.getBody());
 * JsonNode list = root.path("list"); // 또는 실제 응답 구조에 맞게
 *
 * - 리스트로 변환하여 반환
 *
 * [RestTemplate Bean 등록 방법]
 * 별도 Config 클래스에 다음을 추가:
 * 
 * @Configuration
 *                public class AppConfig {
 * @Bean
 *       public RestTemplate restTemplate() {
 *       return new RestTemplate();
 *       }
 *       }
 *
 *       또는 WebClient 사용 가능 (Spring WebFlux 비동기 방식):
 *       WebClient webClient = WebClient.builder().baseUrl(fssBaseUrl).build();
 *
 *       [사용 어노테이션]
 *       - @Service, @RequiredArgsConstructor
 *       - @Value : application-secrets.yml에서 API 키 주입
 */
@Service
@RequiredArgsConstructor
public class FssService {
	
	@Value("${fss.auth-key}")
	private String authKey;
	
	
	public Map<String, Object> getFssDataFromOpenApi(String apiType, String startDate, String endDate) {
		// 1. RestTemplate 생성
		RestTemplate restTemplate = new RestTemplate();

		// 2. StringHttpMessageConverter 객체를 명확히 생성
		StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(Charset.forName("EUC-KR"));

		// 3. 컨버터 리스트의 맨 앞에 추가 (기본 UTF-8 컨버터보다 우선순위를 높임)
		restTemplate.getMessageConverters().add(0, stringConverter);

	    String url = "https://www.fss.or.kr/fss/kr/openApi/api/fcnInfo.jsp" 
	               + "?apiType=json" 
	               + "&startDate=" + (startDate != null ? startDate.replace("-", "") : "20260101") 
	               + "&endDate=" + (endDate != null ? endDate.replace("-", "") : "20260122")
	               + "&authKey=" + authKey;

	    try {
	        // 4. 일단 String으로 받아서 내용을 로그로 확인합니다.
	        String response = restTemplate.getForObject(url, String.class);
	        System.out.println("API Response Raw Data: " + response);

	        // 5. 응답이 JSON 형식이 맞다면 변환 (Jackson Objectmapper 사용 권장)
	        ObjectMapper objectMapper = new ObjectMapper();
	        return objectMapper.readValue(response, Map.class);

	    } catch (Exception e) {
	        System.err.println("데이터 변환 중 에러 발생: " + e.getMessage());
	        // 에러 발생 시 빈 맵 반환 혹은 에러 처리
	        return Collections.singletonMap("error", "데이터를 가져올 수 없습니다. API 키나 날짜 형식을 확인하세요.");
	    }
	}

}
