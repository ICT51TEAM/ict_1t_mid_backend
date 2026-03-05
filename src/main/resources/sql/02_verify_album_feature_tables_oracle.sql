-- [파일 역할]
-- [DB] 사진/앨범 기능이 정상 생성되었는지 확인하는 조회 스크립트입니다.
--
-- [사용 방법]
-- 1) 01_create_album_feature_tables_oracle.sql 실행 후 이 파일 실행
-- 2) 테이블/컬럼/FK/인덱스/데이터를 순서대로 점검
--
-- [실행 흐름(순서)]
-- 1) 테이블 존재 확인
-- 2) 컬럼 구조 확인
-- 3) 제약조건(FK/UNIQUE/CHECK) 확인
-- 4) 인덱스 확인
-- 5) 샘플 조회(최신 10건)

--------------------------------------------------------------------------------
-- 1) 테이블 존재 확인
--------------------------------------------------------------------------------
SELECT table_name
FROM user_tables
WHERE table_name IN ('PHOTO', 'ALBUM', 'ALBUM_PHOTO', 'ALBUM_TAGS')
ORDER BY table_name;

--------------------------------------------------------------------------------
-- 2) 컬럼 구조 확인
--------------------------------------------------------------------------------
SELECT table_name, column_name, data_type, data_length, nullable
FROM user_tab_columns
WHERE table_name IN ('PHOTO', 'ALBUM', 'ALBUM_PHOTO', 'ALBUM_TAGS')
ORDER BY table_name, column_id;

--------------------------------------------------------------------------------
-- 3) 제약조건 확인
--------------------------------------------------------------------------------
SELECT uc.table_name,
       uc.constraint_name,
       uc.constraint_type,
       ucc.column_name,
       uc.search_condition
FROM user_constraints uc
JOIN user_cons_columns ucc
  ON uc.constraint_name = ucc.constraint_name
WHERE uc.table_name IN ('PHOTO', 'ALBUM', 'ALBUM_PHOTO', 'ALBUM_TAGS')
ORDER BY uc.table_name, uc.constraint_name, ucc.position;

--------------------------------------------------------------------------------
-- 4) 인덱스 확인
--------------------------------------------------------------------------------
SELECT table_name, index_name, uniqueness
FROM user_indexes
WHERE table_name IN ('PHOTO', 'ALBUM', 'ALBUM_PHOTO', 'ALBUM_TAGS')
ORDER BY table_name, index_name;

--------------------------------------------------------------------------------
-- 5) 데이터 확인 (최신 10건)
--------------------------------------------------------------------------------
SELECT * FROM (
  SELECT PHOTO_ID, USER_ID, PHOTO_URL, CREATED_AT
  FROM PHOTO
  ORDER BY PHOTO_ID DESC
)
WHERE ROWNUM <= 10;

SELECT * FROM (
  SELECT ALBUM_ID, USER_ID, TITLE, VISIBILITY, LAYOUT_TYPE, CREATED_AT
  FROM ALBUM
  ORDER BY ALBUM_ID DESC
)
WHERE ROWNUM <= 10;

SELECT * FROM (
  SELECT ALBUM_PHOTO_ID, ALBUM_ID, PHOTO_ID, SLOT_INDEX
  FROM ALBUM_PHOTO
  ORDER BY ALBUM_PHOTO_ID DESC
)
WHERE ROWNUM <= 20;

SELECT * FROM (
  SELECT ALBUM_TAG_ID, ALBUM_ID, TAG_ID
  FROM ALBUM_TAGS
  ORDER BY ALBUM_TAG_ID DESC
)
WHERE ROWNUM <= 20;
