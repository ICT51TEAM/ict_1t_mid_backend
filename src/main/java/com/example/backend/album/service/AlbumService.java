package com.example.backend.album.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Objects;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.album.dto.AlbumDetailPhotoDto;
import com.example.backend.album.dto.AlbumDetailResponse;
import com.example.backend.album.dto.AlbumFeedItemResponse;
import com.example.backend.album.dto.CreateAlbumRequest;
import com.example.backend.album.dto.CreateAlbumResponse;
import com.example.backend.album.dto.LatestFrinendAlbumDto;
import com.example.backend.album.entity.AlbumEntity;
import com.example.backend.album.repository.AlbumRepository;
import com.example.backend.albumphoto.entity.AlbumPhotoEntity;
import com.example.backend.albumphoto.repository.AlbumPhotoRepository;
import com.example.backend.albumtag.repository.AlbumTagRepository;
import com.example.backend.badge.dto.AlbumDalgaeDto;
import com.example.backend.badge.repository.BadgeRepository;
import com.example.backend.badge.service.BadgeService;
import com.example.backend.friend.repository.FriendshipRepository;
import com.example.backend.photo.entity.PhotoEntity;
import com.example.backend.photo.repository.PhotoRepository;
import com.example.backend.tag.entity.Tag;
import com.example.backend.tag.repository.TagRepository;
import com.example.backend.user.entity.UserEntity;
import com.example.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/*
[파일 역할]
- [BACK][SERVICE] 앨범 생성/상세 조회 비즈니스 로직을 처리합니다.

[관련 기능(화면/요청)]
- 화면: CreatePhotoAlbumPage.jsx
- 화면: AlbumDetailPage.jsx
- API: POST /api/albums
- API: GET /api/albums/{albumId}
- API: GET /api/albums/layout-types

[실행 흐름(순서)]
1) 요청 검증
2) USER 조회 후 ALBUM 저장(생성 API)
3) photoIds/slotIndexes로 ALBUM_PHOTO 저장
4) tags로 TAGS/ALBUM_TAGS 저장
5) 상세 조회 시 ALBUM + 사진 + 태그를 DTO로 조합
*/
@Service
@RequiredArgsConstructor
public class AlbumService {

    // [BACK] 공개 범위 허용값 목록
    private static final Set<String> ALLOWED_VISIBILITY = Set.of("PUBLIC", "FRIENDS_ONLY", "PRIVATE");

    private final AlbumRepository albumRepository;
    private final AlbumPhotoRepository albumPhotoRepository;
    private final AlbumTagRepository albumTagRepository;
    private final PhotoRepository photoRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final BadgeService badgeService;
    private final BadgeRepository badgeRepository;
    private final FriendshipRepository friendshipRepository;

    // [BACK][API]
    // - 어디서 호출? : AlbumController.createAlbum()
    // - 입력값 : CreateAlbumRequest
    // - 출력값 : CreateAlbumResponse
    @Transactional
    public CreateAlbumResponse createAlbum(CreateAlbumRequest request) {
        validateRequest(request);

        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // [BACK][DB] FK_ALBUM_LAYOUT_TYPE 제약조건을 만족시키기 위해 DB 코드로 정규화
        String normalizedLayoutType = normalizeLayoutType(request.getLayoutType());

        AlbumEntity album = albumRepository.save(
                AlbumEntity.builder()
                        .user(user)
                        .title(request.getTitle().trim())
                        .bodyText(request.getBodyText())
                        .recordDate(request.getRecordDate())
                        .visibility(request.getVisibility())
                        .layoutType(normalizedLayoutType)
                        .build());

        saveAlbumPhotos(album, request.getPhotoIds(), request.getSlotIndexes());
        saveAlbumTags(album, request.getTags());

        return CreateAlbumResponse.builder()
                .albumId(album.getId())
                .message("앨범이 성공적으로 생성되었습니다.")
                .build();
    }

    // [BACK][API]
    // - 어디서 호출? : AlbumController.getAlbumDetail()
    // - 입력값 : albumId
    // - 출력값 : AlbumDetailResponse
    @Transactional(readOnly = true)
    public AlbumDetailResponse getAlbumDetail(Long albumId) {
        if (albumId == null) {
            throw new IllegalArgumentException("albumId는 필수입니다.");
        }

        AlbumEntity album = albumRepository.findById(albumId)
                .orElseThrow(() -> new NoSuchElementException("앨범을 찾을 수 없습니다."));

        List<AlbumDetailPhotoDto> photos = albumPhotoRepository.findByAlbum_IdOrderBySlotIndexAsc(albumId)
                .stream()
                .map(this::toAlbumDetailPhotoDto)
                .collect(Collectors.toList());

        List<String> tags = albumTagRepository.findByAlbum_IdOrderByIdAsc(albumId)
                .stream()
                .map(link -> link.getTag() == null ? null : link.getTag().getName())
                .filter(tagName -> tagName != null && !tagName.isBlank())
                .collect(Collectors.toList());

        // [BACK] 앨범에 달린 달개(이모지 반응) 집계 조회
        List<AlbumDalgaeDto> badges = badgeService.getAlbumDalgaeList(albumId);

        return AlbumDetailResponse.builder()
                .albumId(album.getId())
                .userId(album.getUser() == null ? null : album.getUser().getId())
                .username(album.getUser() == null ? null : album.getUser().getUsername())
                .title(album.getTitle())
                .bodyText(album.getBodyText())
                .recordDate(album.getRecordDate())
                .visibility(album.getVisibility())
                .layoutType(album.getLayoutType())
                .createdAt(album.getCreatedAt())
                .photos(photos)
                .tags(tags)
                .badges(badges)
                .myBadges(List.of())
                .build();
    }

    // [BACK][UTIL]
    // - 어디서 호출? : getAlbumDetail()
    // - 입력값 : AlbumPhotoEntity
    // - 출력값 : AlbumDetailPhotoDto
    private AlbumDetailPhotoDto toAlbumDetailPhotoDto(AlbumPhotoEntity albumPhoto) {
        PhotoEntity photo = albumPhoto.getPhoto();

        return AlbumDetailPhotoDto.builder()
                .photoId(photo == null ? null : photo.getId())
                .photoUrl(photo == null ? null : photo.getPhotoUrl())
                .thumbUrl(photo == null ? null : photo.getThumbUrl())
                .slotIndex(albumPhoto.getSlotIndex())
                .build();
    }

    // [BACK][API]
    // - 어디서 호출? : AlbumController.getAlbumFeed()
    // - 입력값 : type(photo/text/all), friendsOnly, tag
    // - 출력값 : 피드 카드 목록
    @Transactional(readOnly = true)
    public List<AlbumFeedItemResponse> getAlbumFeed(String type, String visibility, String tag,
            Authentication authentication) {
        if (!isSupportedFeedType(type)) {
            return List.of();
        }

        Long currentUserId = authentication != null ? Long.valueOf(authentication.getName()) : null;

        // 글벗 탭: 내가 팔로우하는 사람 ID 목록 미리 조회
        Set<Long> followingIds = new HashSet<>();
        if ("FRIENDS".equalsIgnoreCase(visibility) && currentUserId != null) {
            followingIds = friendshipRepository.findFollowingIdsByUserId(currentUserId);
        }

        List<AlbumEntity> albums = albumRepository.findAllByOrderByCreatedAtDesc();
        List<AlbumFeedItemResponse> result = new ArrayList<>();

        for (AlbumEntity album : albums) {
            Long authorId = album.getUser() == null ? null : album.getUser().getId();
            String vis = album.getVisibility();
            boolean isOwner = Objects.equals(authorId, currentUserId);

            if ("FRIENDS".equalsIgnoreCase(visibility)) {
                // 글벗: PUBLIC 또는 FRIENDS 공개 + 실제 팔로우 관계인 사람 글만
                if (!"PUBLIC".equalsIgnoreCase(vis) && !"FRIENDS".equalsIgnoreCase(vis))
                    continue;
                if (!followingIds.contains(authorId))
                    continue;

            } else if ("MINE".equalsIgnoreCase(visibility)) {
                // 나만 탭: 공개범위와 무관하게 내 글 전체
                if (!isOwner)
                    continue;

            } else if ("PRIVATE".equalsIgnoreCase(visibility)) {
                // PRIVATE 필터: PRIVATE + 내 글만
                if (!"PRIVATE".equalsIgnoreCase(vis))
                    continue;
                if (!isOwner)
                    continue;

            } else {
                // 전체: 다른 사람 글은 PUBLIC만, 본인 글은 공개범위와 무관하게 포함
                if (!"PUBLIC".equalsIgnoreCase(vis) && !isOwner)
                    continue;
            }

            List<String> tags = albumTagRepository.findByAlbum_IdOrderByIdAsc(album.getId())
                    .stream()
                    .map(link -> link.getTag() == null ? null : link.getTag().getName())
                    .filter(tagName -> tagName != null && !tagName.isBlank())
                    .collect(Collectors.toList());

            if (!matchesTagFilter(tag, tags))
                continue;

            String coverImageUrl = albumPhotoRepository.findFirstByAlbum_IdOrderBySlotIndexAsc(album.getId())
                    .map(link -> link.getPhoto() == null ? null : link.getPhoto().getPhotoUrl())
                    .orElse(null);

            result.add(AlbumFeedItemResponse.builder()
                    .id(album.getId())
                    .type("photo")
                    .routeType("album")
                    .imageUrl(coverImageUrl)
                    .title(album.getTitle())
                    .author(album.getUser() == null ? "알수없음" : album.getUser().getUsername())
                    .authorId(authorId)
                    .preview(album.getBodyText() == null ? "" : album.getBodyText())
                    .tags(tags)
                    .badges(List.of())
                    .date(album.getRecordDate() == null ? "" : album.getRecordDate().toString())
                    .build());
        }

        return result;
    }

    // [BACK][UTIL]
    // - 어디서 호출? : getAlbumFeed()
    // - 입력값 : type
    // - 출력값 : 피드 타입 지원 여부
    private boolean isSupportedFeedType(String type) {
        if (type == null || type.isBlank() || "all".equalsIgnoreCase(type)) {
            return true;
        }
        return "photo".equalsIgnoreCase(type);
    }

    // [BACK][UTIL]
    // - 어디서 호출? : getAlbumFeed()
    // - 입력값 : tag(검색어), tags(앨범 태그 목록)
    // - 출력값 : 태그 필터 통과 여부
    private boolean matchesTagFilter(String tag, List<String> tags) {
        if (tag == null || tag.isBlank()) {
            return true;
        }

        String normalized = tag.trim().toLowerCase();
        return tags.stream().anyMatch(t -> t.toLowerCase().contains(normalized));
    }

    // [BACK][API]
    // - 어디서 호출? : AlbumController.getLayoutTypes()
    // - 입력값 : 없음
    // - 출력값 : DB에 등록된 레이아웃 코드 문자열 목록
    public List<String> getAvailableLayoutTypes() {
        return loadLayoutTypeCodesFromDb();
    }

    // [BACK][DB]
    // - 어디서 호출? : createAlbum()
    // - 입력값 : album, photoIds, slotIndexes
    // - 출력값 : 없음(ALBUM_PHOTO 저장)
    private void saveAlbumPhotos(AlbumEntity album, List<Long> photoIds, List<Integer> slotIndexes) {
        List<PhotoEntity> photos = photoRepository.findAllById(photoIds);
        if (photos.size() != photoIds.size()) {
            throw new IllegalArgumentException("존재하지 않는 photoId가 포함되어 있습니다.");
        }

        Map<Long, PhotoEntity> photoMap = photos.stream()
                .collect(Collectors.toMap(PhotoEntity::getId, Function.identity()));

        List<AlbumPhotoEntity> albumPhotos = new ArrayList<>();
        for (int i = 0; i < photoIds.size(); i++) {
            Long photoId = photoIds.get(i);
            Integer slotIndex = slotIndexes.get(i);

            PhotoEntity photo = photoMap.get(photoId);
            if (photo == null) {
                throw new IllegalArgumentException("photoId=" + photoId + "를 찾을 수 없습니다.");
            }

            albumPhotos.add(AlbumPhotoEntity.builder()
                    .album(album)
                    .photo(photo)
                    .slotIndex(slotIndex == null ? i : slotIndex)
                    .build());
        }

        albumPhotoRepository.saveAll(albumPhotos);
    }

    // [BACK][DB]
    // - 어디서 호출? : createAlbum()
    // - 입력값 : album, tags
    // - 출력값 : 없음(TAGS/ALBUM_TAGS 저장)
    private void saveAlbumTags(AlbumEntity album, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }

        // [BACK] 순서를 유지하면서 중복 태그 제거
        Set<String> normalizedTags = tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (String tagName : normalizedTags) {
            Tag tag = findOrCreateTag(tagName);
            linkAlbumAndTagByJdbc(album.getId(), tag.getId());
        }
    }

    // [BACK][DB]
    // - 어디서 호출? : saveAlbumTags()
    // - 입력값 : tagName
    // - 출력값 : 저장/갱신된 Tag 엔티티
    private Tag findOrCreateTag(String tagName) {
        Tag existing = findTagByNameFlexible(tagName);
        if (existing != null) {
            Integer current = existing.getUsageCount() == null ? 0 : existing.getUsageCount();
            existing.setUsageCount(current + 1);
            return tagRepository.save(existing);
        }

        // [BACK][DB] TAG_ID 자동 생성 전략 충돌을 피하기 위해 JDBC insert(컬럼 지정) 사용
        insertTagRowByJdbc(tagName);

        Tag created = findTagByNameFlexible(tagName);
        if (created == null) {
            throw new IllegalStateException("신규 태그 저장에 실패했습니다: " + tagName);
        }
        return created;
    }

    // [BACK][DB]
    // - 어디서 호출? : findOrCreateTag(), insertTagRowByJdbc()
    // - 입력값 : 태그명
    // - 출력값 : Tag 엔티티 또는 null
    private Tag findTagByNameFlexible(String tagName) {
        Tag exact = tagRepository.findByName(tagName).orElse(null);
        if (exact != null) {
            return exact;
        }

        // [BACK][DB] 대소문자/공백 차이를 허용하는 fallback 조회
        try {
            List<Long> ids = jdbcTemplate.queryForList(
                    "SELECT TAG_ID FROM TAGS WHERE LOWER(TRIM(TAG_NAME)) = LOWER(TRIM(?))",
                    Long.class,
                    tagName);
            if (!ids.isEmpty()) {
                return tagRepository.findById(ids.get(0)).orElse(null);
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    // [BACK][DB]
    // - 어디서 호출? : findOrCreateTag()
    // - 입력값 : 신규 태그명
    // - 출력값 : 없음(TAGS 테이블 insert)
    private void insertTagRowByJdbc(String tagName) {
        try {
            jdbcTemplate.update("INSERT INTO TAGS (TAG_NAME, USAGE_COUNT) VALUES (?, ?)", tagName, 1);
        } catch (Exception e) {
            // [BACK] 중복 삽입 경쟁 상황일 수 있으므로, 이미 존재하면 정상 처리로 간주
            Tag already = findTagByNameFlexible(tagName);
            if (already != null) {
                return;
            }
            throw new IllegalStateException("신규 태그 insert 실패: " + tagName + " / " + e.getMessage(), e);
        }
    }

    // [BACK][DB]
    // - 어디서 호출? : saveAlbumTags()
    // - 입력값 : albumId, tagId
    // - 출력값 : 없음(ALBUM_TAGS insert)
    private void linkAlbumAndTagByJdbc(Long albumId, Long tagId) {
        if (albumTagExists(albumId, tagId)) {
            return;
        }

        try {
            Long nextAlbumTagId = getNextAlbumTagId();
            jdbcTemplate.update(
                    "INSERT INTO ALBUM_TAGS (ALBUM_TAG_ID, ALBUM_ID, TAG_ID) VALUES (?, ?, ?)",
                    nextAlbumTagId,
                    albumId,
                    tagId);
        } catch (Exception e) {
            if (albumTagExists(albumId, tagId)) {
                return;
            }
            throw new IllegalStateException(
                    "ALBUM_TAGS 저장 실패: albumId=" + albumId + ", tagId=" + tagId + " / " + e.getMessage(),
                    e);
        }
    }

    // [BACK][DB]
    // - 어디서 호출? : linkAlbumAndTagByJdbc()
    // - 입력값 : albumId, tagId
    // - 출력값 : 연결 존재 여부
    private boolean albumTagExists(Long albumId, Long tagId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ALBUM_TAGS WHERE ALBUM_ID = ? AND TAG_ID = ?",
                Integer.class,
                albumId,
                tagId);
        return count != null && count > 0;
    }

    // [BACK][DB]
    // - 어디서 호출? : linkAlbumAndTagByJdbc()
    // - 입력값 : 없음
    // - 출력값 : 신규 ALBUM_TAG_ID
    private Long getNextAlbumTagId() {
        Long next = jdbcTemplate.queryForObject("SELECT NVL(MAX(ALBUM_TAG_ID), 0) + 1 FROM ALBUM_TAGS", Long.class);
        if (next == null) {
            throw new IllegalStateException("ALBUM_TAG_ID 생성에 실패했습니다.");
        }
        return next;
    }

    // [BACK][VALIDATION]
    // - 어디서 호출? : createAlbum() 시작 시점
    // - 입력값 : CreateAlbumRequest
    // - 출력값 : 없음(검증 실패 시 IllegalArgumentException)
    private void validateRequest(CreateAlbumRequest request) {
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("제목은 필수입니다.");
        }
        if (request.getRecordDate() == null) {
            throw new IllegalArgumentException("recordDate는 필수입니다.");
        }
        if (request.getVisibility() == null || !ALLOWED_VISIBILITY.contains(request.getVisibility())) {
            throw new IllegalArgumentException("visibility는 PUBLIC, FRIENDS_ONLY, PRIVATE 중 하나여야 합니다.");
        }
        if (request.getLayoutType() == null || request.getLayoutType().isBlank()) {
            throw new IllegalArgumentException("layoutType은 필수입니다.");
        }
        if (request.getPhotoIds() == null || request.getPhotoIds().isEmpty()) {
            throw new IllegalArgumentException("photoIds는 최소 1개 이상이어야 합니다.");
        }
        if (request.getSlotIndexes() == null || request.getSlotIndexes().isEmpty()) {
            throw new IllegalArgumentException("slotIndexes는 최소 1개 이상이어야 합니다.");
        }
        if (request.getPhotoIds().size() != request.getSlotIndexes().size()) {
            throw new IllegalArgumentException("photoIds와 slotIndexes 길이는 동일해야 합니다.");
        }
    }

    // [BACK][DB]
    // - 어디서 호출? : normalizeLayoutType(), getAvailableLayoutTypes()
    // - 입력값 : 없음
    // - 출력값 : FK_ALBUM_LAYOUT_TYPE의 부모 테이블에 등록된 코드 목록
    private List<String> loadLayoutTypeCodesFromDb() {
        try {
            String parentTableSql = """
                    SELECT pk.table_name
                    FROM user_constraints fk
                    JOIN user_constraints pk
                      ON fk.r_constraint_name = pk.constraint_name
                    WHERE fk.constraint_name = 'FK_ALBUM_LAYOUT_TYPE'
                    """;

            String parentColumnSql = """
                    SELECT pk_cols.column_name
                    FROM user_constraints fk
                    JOIN user_constraints pk
                      ON fk.r_constraint_name = pk.constraint_name
                    JOIN user_cons_columns pk_cols
                      ON pk.constraint_name = pk_cols.constraint_name
                    WHERE fk.constraint_name = 'FK_ALBUM_LAYOUT_TYPE'
                    ORDER BY pk_cols.position
                    """;

            String parentTable = jdbcTemplate.queryForObject(parentTableSql, String.class);
            String parentColumn = jdbcTemplate.queryForObject(parentColumnSql, String.class);

            if (parentTable == null || parentColumn == null) {
                return List.of();
            }

            String selectCodesSql = "SELECT " + parentColumn + " FROM " + parentTable + " ORDER BY 1";
            return jdbcTemplate.queryForList(selectCodesSql, String.class);
        } catch (Exception e) {
            // [BACK] 메타데이터 조회 실패 시 빈 목록 반환 (하위 로직에서 fallback 처리)
            return List.of();
        }
    }

    // [BACK][UTIL]
    // - 어디서 호출? : createAlbum()
    // - 입력값 : 요청의 layoutType 문자열
    // - 출력값 : DB FK를 만족하는 실제 코드값
    private String normalizeLayoutType(String requestedLayoutType) {
        List<String> dbCodes = loadLayoutTypeCodesFromDb();
        if (dbCodes.isEmpty()) {
            return requestedLayoutType;
        }

        // 1) 완전 일치(대소문자 무시) 우선
        for (String code : dbCodes) {
            if (code.equalsIgnoreCase(requestedLayoutType)) {
                return code;
            }
        }

        // 2) 프론트 별칭 -> 후보 코드 매핑
        Map<String, List<String>> aliasCandidates = Map.of(
                "single", List.of("SINGLE", "1", "GRID_2", "GRID_4"),
                "horizontal-two", List.of("GRID_2", "SINGLE", "1"),
                "vertical-two", List.of("GRID_2", "SINGLE", "1"),
                "grid", List.of("GRID_4", "GRID_2", "SINGLE", "1"));

        List<String> candidates = aliasCandidates.getOrDefault(requestedLayoutType.toLowerCase(), List.of());
        for (String candidate : candidates) {
            for (String code : dbCodes) {
                if (code.equalsIgnoreCase(candidate)) {
                    return code;
                }
            }
        }

        throw new IllegalArgumentException("layoutType 허용값: " + String.join(", ", dbCodes));
    }

    // [BACK][API]
    // - 어디서 호출? : AlbumController.updateAlbum()
    // - 입력값 : albumId, title, bodyText, visibility, authentication
    // - 출력값 : AlbumDetailResponse (수정된 앨범 상세)
    @Transactional
    public AlbumDetailResponse updateAlbum(Long albumId, String title, String bodyText, String visibility,
            Authentication authentication) {
        AlbumEntity album = albumRepository.findById(albumId)
                .orElseThrow(() -> new NoSuchElementException("앨범을 찾을 수 없습니다."));

        Long userId = Long.valueOf(authentication.getName());
        if (!album.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("본인의 게시글만 수정할 수 있습니다.");
        }

        if (title != null && !title.trim().isEmpty()) {
            album.setTitle(title.trim());
        }
        if (bodyText != null) {
            album.setBodyText(bodyText);
        }
        if (visibility != null && ALLOWED_VISIBILITY.contains(visibility)) {
            album.setVisibility(visibility);
        }

        albumRepository.save(album);
        return getAlbumDetail(albumId);
    }

    // [BACK][API]
    // - 어디서 호출? : AlbumController.deleteAlbum()
    // - 입력값 : albumId, authentication
    // - 출력값 : 없음 (삭제 처리)
    @Transactional
    public void deleteAlbum(Long albumId, Authentication authentication) {
        AlbumEntity album = albumRepository.findById(albumId)
                .orElseThrow(() -> new NoSuchElementException("앨범을 찾을 수 없습니다."));

        Long userId = Long.valueOf(authentication.getName());
        if (!album.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("본인의 게시글만 삭제할 수 있습니다.");
        }

        // FK 의존 순서에 따라 삭제: badge → albumTag → albumPhoto → album
        badgeRepository.deleteByAlbumId(albumId);
        albumTagRepository.deleteByAlbum_Id(albumId);
        albumPhotoRepository.deleteByAlbum_Id(albumId);
        albumRepository.delete(album);
    }

    // 글벗의 최신 게시글 조회
    public LatestFrinendAlbumDto getLatestFriendStory(Long myId, Boolean friendsOnly, String tag) {
        // [BACK] 현재 앨범 피드는 사진형(photo)만 지원합니다.
        if (!isSupportedFeedType("photo")) {
            return null;
        }
        // 모든 앨범을 최신순으로 가져 오기
        List<AlbumEntity> albums = albumRepository.findAllByOrderByCreatedAtDesc();

        for (AlbumEntity album : albums) {
            // 내가 작성한
            // [BACK] friendsOnly=true일 때는 private 앨범을 제외합니다.
            if (Boolean.TRUE.equals(friendsOnly) && "PRIVATE".equalsIgnoreCase(album.getVisibility())) {
                continue;
            }

            List<String> tags = albumTagRepository.findByAlbum_IdOrderByIdAsc(album.getId())
                    .stream()
                    .map(link -> link.getTag() == null ? null : link.getTag().getName())
                    .filter(tagName -> tagName != null && !tagName.isBlank())
                    .collect(Collectors.toList());

            // [BACK] 태그 검색어가 있으면 태그 목록에 포함된 앨범만 반환합니다.
            if (!matchesTagFilter(tag, tags)) {
                continue;
            }

            String coverImageUrl = albumPhotoRepository.findFirstByAlbum_IdOrderBySlotIndexAsc(album.getId())
                    .map(link -> link.getPhoto() == null ? null : link.getPhoto().getPhotoUrl())
                    .orElse(null);

            return LatestFrinendAlbumDto.builder()
                    .postId(album.getId())
                    .albumId(album.getId())
                    .title(album.getTitle())
                    .author(album.getUser() == null ? "알수없음" : album.getUser().getUsername())
                    .authorBadge(album.getUser() != null ? "📝" : "👤")
                    .date(album.getRecordDate() == null ? "" : album.getRecordDate().toString())
                    .build();
        }

        return null;
    }
}
