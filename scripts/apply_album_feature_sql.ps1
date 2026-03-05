<##
[파일 역할]
- [BACK][DB] Oracle DB에 앨범 기능 SQL을 적용하는 자동 실행 스크립트입니다.

[관련 기능(화면/요청)]
- backend/src/main/resources/sql/01_create_album_feature_tables_oracle.sql 실행

[실행 흐름(순서)]
1) UTF-8 콘솔 출력 설정
2) sqlplus 존재 확인
3) Oracle 접속 문자열 구성
4) DDL SQL 실행
5) 성공/실패 결과 출력
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

# [BACK][DB] SQL 파일 경로를 현재 스크립트 기준으로 계산합니다.
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$sqlFile = Join-Path $scriptRoot "..\src\main\resources\sql\01_create_album_feature_tables_oracle.sql"
$sqlFile = (Resolve-Path $sqlFile).Path

Write-Host "[INFO] SQL 파일: $sqlFile"
Write-Host "[INFO] 접속 URL: $DbUrl"

if (-not (Get-Command sqlplus -ErrorAction SilentlyContinue)) {
    throw "sqlplus를 찾을 수 없습니다. Oracle Client 설치를 확인하세요."
}

$cmd = "@$sqlFile"
$cmd | sqlplus -L "$DbUser/$DbPassword@$DbUrl"

if ($LASTEXITCODE -ne 0) {
    throw "DDL 실행에 실패했습니다. 종료 코드: $LASTEXITCODE"
}

Write-Host "[DONE] DDL 적용 완료"
