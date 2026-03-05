<##
[파일 역할]
- [FRONT/BACK][API] 사진 업로드 API와 앨범 생성 API를 순서대로 테스트하는 스크립트입니다.

[관련 기능(화면/요청)]
- POST /api/photos/upload
- POST /api/albums

[실행 흐름(순서)]
1) UTF-8 콘솔 출력 설정
2) 테스트 이미지 파일 경로 확인
3) HttpClient + MultipartFormDataContent로 /api/photos/upload 호출
4) 응답 photoId 목록 추출
5) /api/albums 호출
6) albumId/message 출력
##>

param(
    [Parameter(Mandatory = $true)]
    [string]$ImagePath,

    [int]$UserId = 1,

    [string]$BaseUrl = "http://localhost:8080/api"
)

$ErrorActionPreference = "Stop"

# [공통] 콘솔 한글 깨짐 방지
try { chcp 65001 > $null } catch {}
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# [API] 파일 존재 여부 확인
if (-not (Test-Path $ImagePath)) {
    throw "이미지 파일이 존재하지 않습니다: $ImagePath"
}

Write-Host "[INFO] 업로드 파일: $ImagePath"
Write-Host "[INFO] API 기본 주소: $BaseUrl"

# [API] Step 1: 사진 업로드 (PowerShell 5 호환 멀티파트)
$uploadUrl = "$BaseUrl/photos/upload"
Write-Host "[STEP1] POST $uploadUrl"

Add-Type -AssemblyName System.Net.Http
$httpClient = New-Object System.Net.Http.HttpClient

try {
    $multipart = New-Object System.Net.Http.MultipartFormDataContent

    # [API] files 파트 추가
    $bytes = [System.IO.File]::ReadAllBytes($ImagePath)
    $byteContent = New-Object System.Net.Http.ByteArrayContent -ArgumentList (, $bytes)
    $ext = [System.IO.Path]::GetExtension($ImagePath).ToLowerInvariant(); $mime = if ($ext -eq '.jpg' -or $ext -eq '.jpeg') { 'image/jpeg' } elseif ($ext -eq '.webp') { 'image/webp' } else { 'image/png' }; $byteContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse($mime)
    $multipart.Add($byteContent, "files", [System.IO.Path]::GetFileName($ImagePath))

    # [API] userId 파트 추가
    $userIdContent = New-Object System.Net.Http.StringContent("$UserId")
    $multipart.Add($userIdContent, "userId")

    $uploadHttpResponse = $httpClient.PostAsync($uploadUrl, $multipart).Result
    $uploadBody = $uploadHttpResponse.Content.ReadAsStringAsync().Result

    if (-not $uploadHttpResponse.IsSuccessStatusCode) {
        throw "사진 업로드 실패: HTTP $($uploadHttpResponse.StatusCode) / $uploadBody"
    }

    $uploadResponse = $uploadBody | ConvertFrom-Json

    if (-not $uploadResponse.photos -or $uploadResponse.photos.Count -eq 0) {
        throw "사진 업로드 응답에서 photos 배열을 찾을 수 없습니다."
    }

    $photoIds = @($uploadResponse.photos | ForEach-Object { $_.photoId })
    $slotIndexes = 0..($photoIds.Count - 1)

    Write-Host "[STEP1-DONE] photoIds: $($photoIds -join ', ')"

    # [API] Step 2: 앨범 생성
    $albumUrl = "$BaseUrl/albums"
    Write-Host "[STEP2] POST $albumUrl"

    $albumPayload = @{
        userId      = $UserId
        title       = "API 테스트 앨범"
        bodyText    = "PowerShell 스크립트로 생성한 테스트 앨범입니다."
        recordDate  = (Get-Date).ToString("yyyy-MM-dd")
        visibility  = "PUBLIC"
        layoutType  = if ($photoIds.Count -ge 4) { "grid" } else { "single" }
        photoIds    = $photoIds
        slotIndexes = $slotIndexes
        tags        = @("테스트", "자동화")
    } | ConvertTo-Json -Depth 5

    $albumResponse = Invoke-RestMethod -Uri $albumUrl -Method Post -ContentType "application/json; charset=utf-8" -Body $albumPayload

    Write-Host "[STEP2-DONE] albumId: $($albumResponse.albumId)"
    Write-Host "[DONE] message: $($albumResponse.message)"
}
finally {
    $httpClient.Dispose()
}

