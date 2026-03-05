package com.example.backend.global.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * [공통 베이스 엔티티 (Base Entity)]
 * - 모든 엔티티가 상속받아 createdAt, updatedAt 을 자동 관리합니다.
 *
 * [사용법]
 * 1. 다른 엔티티에서 extends BaseEntity 로 상속
 * 2. BackendApplication 클래스에 @EnableJpaAuditing 어노테이션 추가 필수
 *
 * [필요한 변수]
 * - createdAt (LocalDateTime) : 레코드 생성 시 자동 기록 (@CreatedDate)
 * - updatedAt (LocalDateTime) : 레코드 수정 시 자동 갱신 (@LastModifiedDate)
 *
 * [사용 어노테이션]
 * - @MappedSuperclass : 이 클래스 자체는 테이블이 아니고, 상속받는 엔티티에 컬럼을 제공
 * - @EntityListeners(AuditingEntityListener.class) : JPA Auditing 활성화
 * - @Getter : Lombok — getter 자동 생성
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
