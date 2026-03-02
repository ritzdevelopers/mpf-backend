package com.mypropertyfact.estate.services.impl;

import com.mypropertyfact.estate.common.FileUtils;
import com.mypropertyfact.estate.configs.UploadProperties;
import com.mypropertyfact.estate.dtos.BannerUploadDto;
import com.mypropertyfact.estate.dtos.HomeBannerDto;
import com.mypropertyfact.estate.entities.HomeBanner;
import com.mypropertyfact.estate.repositories.HomeBannerRepository;
import com.mypropertyfact.estate.services.HomeBannerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeBannerServiceImpl implements HomeBannerService {
    private final HomeBannerRepository homeBannerRepository;
    private final FileUtils fileUtils;
    private final UploadProperties uploadProperties;

    @Override
    public void addBanner(BannerUploadDto bannerData) {
        if (bannerData != null) {
            int desktopImages = saveHomeBanner(bannerData.getDesktopImages(), "desktop", bannerData);
            if(desktopImages > 0) {
                log.info("Desktop image uploaded count: {}", desktopImages);
            }
            int mobileImages = saveHomeBanner(bannerData.getMobileImages(), "mobile", bannerData);
            if(mobileImages > 0) {
                log.info("Mobile image uploaded count: {}", mobileImages);
            }
            int tabletImages = saveHomeBanner(bannerData.getTabletImages(), "tablet", bannerData);
            if(tabletImages > 0) {
                log.info("Tablet image uploaded count: {}", tabletImages);
            }
        }
    }

    private int saveHomeBanner(
            List<MultipartFile> bannerImages,
            String deviceType,
            BannerUploadDto bannerData
    ) {
        if (bannerImages == null || bannerImages.isEmpty()) {
            return 0;
        }

        UploadProperties.HomeBannerConfig config = uploadProperties.getHomeBannerConfig();

        int width;
        int height;

        switch (deviceType) {
            case "mobile" -> {
                width = config.getMobileWidth();
                height = config.getMobileHeight();
            }
            case "tablet" -> {
                width = config.getTabletWidth();
                height = config.getTabletHeight();
            }
            default -> {
                width = config.getDesktopWidth();
                height = config.getDesktopHeight();
            }
        }

        String destination = Paths.get(
                uploadProperties.getDir(),
                config.getFolderName()
        ).toString();

        int savedCount = 0;

        for (MultipartFile bannerImage : bannerImages) {

            if (bannerImage.isEmpty()
                    || !fileUtils.isTypeImage(bannerImage)
                    || !fileUtils.checkFileSize(bannerImage)) {
                continue;
            }

            try {

                BufferedImage image = ImageIO.read(bannerImage.getInputStream());

                if (image == null) {
                    continue;
                }

                boolean validAspectRatio = fileUtils.isValidAspectRatio(
                        bannerImage.getInputStream(),
                        width,
                        height
                );

                if (!validAspectRatio) {
                    continue;
                }

                String imageName = fileUtils.saveDesktopImageWithResize(
                        bannerImage,
                        destination,
                        width,
                        height,
                        config.getDefaultQuality()
                );

                if (imageName == null) continue;

                String bannerLink = bannerData.getBanner() != null
                        ? bannerData.getBanner().getBannerLink()
                        : null;

                HomeBanner banner = new HomeBanner();
                banner.setImageName(imageName);
                banner.setDeviceType(deviceType);
                banner.setImageAlt(fileUtils.generateImageAltTag(bannerImage));
                banner.setImageId(UUID.randomUUID().toString());
                banner.setImageHeight(height);
                banner.setImageWidth(width);
                banner.setBannerLink(bannerLink);
                homeBannerRepository.save(banner);
                savedCount++;
            } catch (Exception e) {
                log.error("Banner upload failed", e);
            }
        }
        return savedCount;
    }

    @Override
    public List<HomeBannerDto> getAllBanners() {
        return homeBannerRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private HomeBannerDto toDto(HomeBanner b) {
        HomeBannerDto dto = new HomeBannerDto();
        dto.setId(b.getId());
        dto.setImageId(b.getImageId());
        dto.setImageName(b.getImageName());
        dto.setImageAlt(b.getImageAlt());
        dto.setImageHeight(String.valueOf(b.getImageHeight()));
        dto.setImageWidth(String.valueOf(b.getImageWidth()));
        dto.setDeviceType(b.getDeviceType());
        dto.setBannerLink(b.getBannerLink());
        return dto;
    }

    @Override
    public HomeBanner findByImageId(Integer imageId) {
        return homeBannerRepository.findById(imageId).orElse(null);
    }

    @Override
    @Transactional
    public void deleteImage(Integer imageId) {
        homeBannerRepository.findById(imageId).ifPresent(banner -> {
            String imageName = banner.getImageName();
            if (imageName != null && !imageName.isEmpty()) {
                UploadProperties.HomeBannerConfig config = uploadProperties.getHomeBannerConfig();
                if (config != null) {
                    String destination = Paths.get(
                            uploadProperties.getDir(),
                            config.getFolderName()
                    ).toString();
                    boolean deleted = fileUtils.deleteFileFromDestination(imageName, destination);
                    if (deleted) {
                        log.info("Deleted home banner image from disk: {}", imageName);
                    } else {
                        log.warn("Could not delete home banner image file (may not exist): {}", imageName);
                    }
                }
            }
            homeBannerRepository.delete(banner);
        });
    }

    @Override
    @Transactional
    public void updateBanner(Integer id, String bannerLink, String imageAlt) {
        homeBannerRepository.findById(id).ifPresent(banner -> {
            if (bannerLink != null) banner.setBannerLink(bannerLink);
            if (imageAlt != null) banner.setImageAlt(imageAlt);
            homeBannerRepository.save(banner);
        });
    }
}
