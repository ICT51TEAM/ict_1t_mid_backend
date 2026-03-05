package com.example.backend.photo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
[파일 역할]
- [BACK][API] 업로드된 사진 1건의 응답 데이터를 담는 DTO입니다.

[관련 기능(화면/요청)]
- POST /api/photos/upload 응답 photos 배열의 내부 객체로 사용됩니다.

[실행 흐름(순서)]
1) PhotoService가 DB 저장 결과(id/url)를 얻음
2) PhotoDto로 변환
3) PhotoUploadResponse.photos 배열에 포함
*/
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoDto {

    // [API] 저장된 사진 PK
    private Long photoId;

    // [API] 원본 이미지 URL
    private String photoUrl;

    // [API] 썸네일 URL (현재는 photoUrl과 동일)
    private String thumbUrl;
}
