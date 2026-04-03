package com.mypropertyfact.estate.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "web_story_category")
@Data
public class WebStoryCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(unique = true)
    private String categoryName;
    private String categoryDescription;
    @Column(columnDefinition = "TEXT")
    private String metaDescription;
    @Column(columnDefinition = "TEXT")
    private String metaKeywords;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "webStoryCategory", cascade = CascadeType.ALL)
    private List<WebStory> webStories;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
