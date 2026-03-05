# 앨범 기능 배포/검증 가이드 (UTF-8)

## 1) 목적
이 문서는 아래 기능을 실제 환경에서 검증하기 위한 실행 순서를 정리합니다.

- 사진 업로드 API: `POST /api/photos/upload`
- 앨범 생성 API: `POST /api/albums`
- DB 테이블: `PHOTO`, `ALBUM`, `ALBUM_PHOTO`, `ALBUM_TAGS`

## 2) 사전 조건
1. 백엔드 서버 포트: `8080`
2. DB URL: `jdbc:oracle:thin:@//100.91.129.24:1521/XEPDB1`
3. Oracle Client(sqlplus) 설치
4. 백엔드 서버 실행 상태 (`http://localhost:8080/api` 응답 가능)

## 3) DDL 적용
아래 스크립트는 테이블/제약조건/인덱스를 생성합니다.

```powershell
cd D:\SJY\Workspace\Midproject\backend\scripts
.\apply_album_feature_sql.ps1 -DbUser SCOTT -DbPassword tiger -DbUrl "//100.91.129.24:1521/XEPDB1"
```

실행 SQL 파일:
- `backend/src/main/resources/sql/01_create_album_feature_tables_oracle.sql`

## 4) DDL 검증
테이블/컬럼/FK/인덱스/샘플 데이터를 조회합니다.

```powershell
cd D:\SJY\Workspace\Midproject\backend\scripts
.\verify_album_feature_sql.ps1 -DbUser SCOTT -DbPassword tiger -DbUrl "//100.91.129.24:1521/XEPDB1"
```

실행 SQL 파일:
- `backend/src/main/resources/sql/02_verify_album_feature_tables_oracle.sql`

## 5) API 기능 테스트
테스트 이미지 1장을 지정해서 업로드 + 앨범 생성을 순차 실행합니다.

```powershell
cd D:\SJY\Workspace\Midproject\backend\scripts
.\test_album_feature_api.ps1 -ImagePath "C:\temp\test.jpg" -UserId 1 -BaseUrl "http://localhost:8080/api"
```

정상 결과 예시:
- `[STEP1-DONE] photoIds: ...`
- `[STEP2-DONE] albumId: ...`
- `[DONE] message: 앨범이 성공적으로 생성되었습니다.`

## 6) 문제 해결
1. `ORA-12546: TNS:허용이 거부되었습니다`
- DB 서버 리스너/방화벽 정책 또는 계정 권한 문제일 가능성이 큽니다.
- 서버 접근 허용 IP, 리스너 상태, SCOTT 계정 접속 권한을 DBA와 확인하세요.

2. `Invoke-RestMethod` 401/403
- 현재 구현은 `/photos/**`, `/albums/**`를 permitAll로 열어두었지만, 보안 설정이 바뀌면 토큰 필요할 수 있습니다.

3. 업로드 실패(파일 확장자)
- 허용 확장자: `jpg`, `jpeg`, `png`, `webp`
- 최대 파일 크기: `10MB`
