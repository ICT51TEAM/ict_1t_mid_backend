package com.example.backend.album.dto;

import java.util.List;

public class AlbumUpdateRequests {
    private String title;
    private String bodyText;
    private String visibility;
    private String recordDate;
    private String layoutType;
    private List<Long> photoIds;    
    private List<Integer> slotIndexes; 
    private List<String> tags;

    // 1. 필수: 기본 생성자
    public AlbumUpdateRequests() {}

    // 2. 필수: Getter/Setter (Jackson이 이 메서드들을 보고 타입을 판단합니다)
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBodyText() { return bodyText; }
    public void setBodyText(String bodyText) { this.bodyText = bodyText; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public String getRecordDate() { return recordDate; }
    public void setRecordDate(String recordDate) { this.recordDate = recordDate; }

    public String getLayoutType() { return layoutType; }
    public void setLayoutType(String layoutType) { this.layoutType = layoutType; }

    public List<Long> getPhotoIds() { return photoIds; }
    public void setPhotoIds(List<Long> photoIds) { this.photoIds = photoIds; }

    public List<Integer> getSlotIndexes() { return slotIndexes; }
    public void setSlotIndexes(List<Integer> slotIndexes) { this.slotIndexes = slotIndexes; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
