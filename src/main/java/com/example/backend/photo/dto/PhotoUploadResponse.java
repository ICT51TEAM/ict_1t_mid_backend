package com.example.backend.photo.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
[파일 역할]
- [BACK][API] 사진 업로드 응답의 최상위 객체를 표현하는 DTO입니다.

[관련 기능(화면/요청)]
- POST /api/photos/upload

[실행 흐름(순서)]
1) 업로드된 PhotoDto 목록 생성
2) PhotoUploadResponse.photos에 담기
3) 컨트롤러에서 JSON 응답으로 반환
*/
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoUploadResponse {

    // [API] 업로드된 사진 목록 (필드명 고정: photos)
    private List<PhotoDto> photos;
}
