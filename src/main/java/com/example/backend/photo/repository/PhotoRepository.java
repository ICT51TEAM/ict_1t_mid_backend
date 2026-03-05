package com.example.backend.photo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.photo.entity.PhotoEntity;

/*
[파일 역할]
- [BACK][DB] PHOTO 테이블 CRUD를 담당하는 Repository입니다.

[관련 기능(화면/요청)]
- PhotoService에서 사진 저장/조회 시 사용합니다.

[실행 흐름(순서)]
1) Service에서 photoRepository.save() 호출
2) JPA가 PHOTO 테이블 insert 실행
3) 저장된 엔티티 반환
*/
public interface PhotoRepository extends JpaRepository<PhotoEntity, Long> {
}
