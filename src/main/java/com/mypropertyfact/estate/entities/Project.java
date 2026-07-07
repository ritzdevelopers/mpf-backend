package com.mypropertyfact.estate.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@NamedEntityGraph(
        name = "Project.withAllRelations",
        attributeNodes = {
                @NamedAttributeNode("city"),
                @NamedAttributeNode("builder"),
                @NamedAttributeNode("projectTypes"),
                @NamedAttributeNode("projectStatus"),
                @NamedAttributeNode("projectBanners"),
                @NamedAttributeNode("floorPlans"),
                @NamedAttributeNode("amenities"),
                @NamedAttributeNode("projectsAbouts"),
                @NamedAttributeNode("projectWalkthroughs"),
                @NamedAttributeNode("locationBenefits"),
                @NamedAttributeNode("projectGalleries"),
                @NamedAttributeNode("projectFaqs")
        }
)
@Entity
@Getter
@Setter
@Table(name = "projects")
@ToString(exclude = {
        "city", "builder", "projectTypes", "projectStatus",
        "projectBanners", "floorPlans", "amenities", "projectsAbouts",
        "projectWalkthroughs", "locationBenefits", "projectGalleries", "projectFaqs"
})
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String metaTitle;
    @Lob
    private String metaKeyword;
    @Lob
    private String metaDescription;
    private String projectName;
    private String projectLocality;
    private String projectConfiguration;
    private String projectPrice;
    private String ivrNo;
    private String locationMap;
    private String reraNo;
    private String reraQr;
    private String reraWebsite;
    private String projectLogo;
    private String projectThumbnail;
    private String slugURL;
    private boolean showFeaturedProperties;
    private String projectThumbnailAltTag;
    private String projectLogoAltTag;
    private String locationMapAltTag;
    private boolean status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_Id")
    @JsonIgnore
    private City city;

    @Lob
    private String amenityDesc;
    @Lob
    private String floorPlanDesc;
    @Lob
    private String locationDesc;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "project_amenities",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    @JsonIgnore
    private Set<Amenity> amenities;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<ProjectBanner> projectBanners;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<FloorPlan> floorPlans;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
    @OrderBy("id DESC")
    @JsonIgnore
    private List<ProjectsAbout> projectsAbouts = new ArrayList<>();

    @JsonIgnore
    public ProjectsAbout getProjectsAbout() {
        return projectsAbouts == null || projectsAbouts.isEmpty()
                ? null
                : projectsAbouts.get(0);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "builder_id")
    @JsonIgnore
    private Builder builder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_type")
    @JsonIgnore
    private ProjectTypes projectTypes;

    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
    @OrderBy("id DESC")
    @JsonIgnore
    private List<ProjectWalkthrough> projectWalkthroughs = new ArrayList<>();

    @JsonIgnore
    public ProjectWalkthrough getProjectWalkthrough() {
        return projectWalkthroughs == null || projectWalkthroughs.isEmpty()
                ? null
                : projectWalkthroughs.get(0);
    }

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<LocationBenefit> locationBenefits;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<ProjectGallery> projectGalleries;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<ProjectFaqs> projectFaqs;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_status_id")
    @JsonIgnore
    private ProjectStatus projectStatus;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ProjectMobileBanner> projectMobileBanners;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ProjectDesktopBanner> projectDesktopBanners;
}
