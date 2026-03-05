<##
[파일 역할]
- [BACK][DB] 앨범 기능 테이블/제약조건/인덱스 검증 SQL을 실행하는 스크립트입니다.

[관련 기능(화면/요청)]
- backend/src/main/resources/sql/02_verify_album_feature_tables_oracle.sql 실행

[실행 흐름(순서)]
1) UTF-8 콘솔 출력 설정
2) sqlplus 존재 확인
3) Oracle 접속
4) 검증 SQL 실행
5) 콘솔에서 결과 확인
##>

param(
    [string]$DbUser = "SCOTT",
    [string]$DbPassword = "tiger",
    [string]$DbUrl = "//100.91.129.24:1521/XEPDB1"
)

$ErrorActionPreference = "Stop"

# [공통] 콘솔 한글 깨짐 방지
try { chcp 65001 > $null } catch {}
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$sqlFile = Join-Path $scriptRoot "..\src\main\resources\sql\02_verify_album_feature_tables_oracle.sql"
$sqlFile = (Resolve-Path $sqlFile).Path

Write-Host "[INFO] 검증 SQL 파일: $sqlFile"
Write-Host "[INFO] 접속 URL: $DbUrl"

if (-not (Get-Command sqlplus -ErrorAction SilentlyContinue)) {
    throw "sqlplus를 찾을 수 없습니다. Oracle Client 설치를 확인하세요."
}

$cmd = "@$sqlFile"
$cmd | sqlplus -L "$DbUser/$DbPassword@$DbUrl"

if ($LASTEXITCODE -ne 0) {
    throw "검증 SQL 실행에 실패했습니다. 종료 코드: $LASTEXITCODE"
}

Write-Host "[DONE] 검증 SQL 실행 완료"
