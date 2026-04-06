package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.dtos.BannerUploadDto;
import com.mypropertyfact.estate.dtos.HomeBannerDto;
import com.mypropertyfact.estate.dtos.HomeBannerUpdateDto;
import com.mypropertyfact.estate.dtos.SuccessResponse;
import com.mypropertyfact.estate.services.HomeBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/v1/home-banner")
@RequiredArgsConstructor
public class HomeBannerController {
    private final HomeBannerService homeBannerService;

    @GetMapping
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("home-banner API");
    }

    @GetMapping("/all")
    public ResponseEntity<List<HomeBannerDto>> getAllBanners() {
        return ResponseEntity.ok(homeBannerService.getAllBanners());
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_WEBSITE')")
    public ResponseEntity<SuccessResponse> deleteBanner(@PathVariable Integer id) {
        homeBannerService.deleteImage(id);
        return ResponseEntity.ok(new SuccessResponse(1, "Banner deleted successfully"));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_WEBSITE')")
    public ResponseEntity<SuccessResponse> updateBanner(@PathVariable Integer id, @RequestBody HomeBannerUpdateDto dto) {
        homeBannerService.updateBanner(id, dto.getBannerLink(), dto.getImageAlt());
        return ResponseEntity.ok(new SuccessResponse(1, "Banner updated successfully"));
    }

    @PostMapping("/add-banners")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_WEBSITE')")
    public ResponseEntity<SuccessResponse> addBanners(@ModelAttribute BannerUploadDto uploadDto) {
        if (uploadDto == null) {
            return ResponseEntity.badRequest()
                    .body(new SuccessResponse(0, "Request body is required"));
        }
        if (!hasAnyImages(uploadDto)) {
            return ResponseEntity.badRequest()
                    .body(new SuccessResponse(0, "At least one image (desktop, mobile, or tablet) is required"));
        }
        try {
            homeBannerService.addBanner(uploadDto);
            return ResponseEntity.ok(new SuccessResponse(1, "Banner added successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new SuccessResponse(0, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new SuccessResponse(0, "Failed to add banner: " + e.getMessage()));
        }
    }

    private static boolean hasAnyImages(BannerUploadDto dto) {
        return Stream.of(
                dto.getDesktopImages(),
                dto.getMobileImages(),
                dto.getTabletImages()
        ).anyMatch(list -> list != null && !list.isEmpty());
    }
}
