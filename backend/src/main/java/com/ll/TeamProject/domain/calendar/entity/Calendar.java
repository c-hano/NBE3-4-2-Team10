package com.ll.TeamProject.domain.calendar.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.ll.TeamProject.domain.calendar.dto.CalendarUpdateDto;
import com.ll.TeamProject.domain.schedule.entity.Schedule;
import com.ll.TeamProject.domain.user.entity.SiteUser;
import com.ll.TeamProject.global.jpa.entity.BaseTime;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
public class Calendar extends BaseTime {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private SiteUser user; // 캘린더 소유 사용자 ID

    private String name; // 캘린더 이름

    private String description; // 캘린더 설명

    @OneToMany(mappedBy = "calendar", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SharedCalendar> sharedUsers = new ArrayList<>(); // 공유된 사용자 정보

    @OneToMany(mappedBy = "calendar", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messageList = new ArrayList<>();

    // 커스텀 메서드로 필드 값 설정
    public void updateName(String name) {
        this.name = name;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    protected Calendar() {
    }

    // 비즈니스 생성자
    public Calendar(SiteUser user, String name, String description) {
        this.user = user;
        this.name = name;
        this.description = description;
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Calendar 엔티티의 update 메서드 수정
    public Calendar update(CalendarUpdateDto updateDto) {
        this.name = updateDto.getName();
        this.description = updateDto.getDescription();  // description도 업데이트
        return this;
    }
    @JsonManagedReference
    @OneToMany(mappedBy = "calendar", cascade = CascadeType.REMOVE)
    private List<Schedule> schedules = new ArrayList<>();
}