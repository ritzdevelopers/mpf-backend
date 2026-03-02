package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.BannerUploadDto;
import com.mypropertyfact.estate.dtos.HomeBannerDto;
import com.mypropertyfact.estate.entities.HomeBanner;

import java.util.List;

public interface HomeBannerService {
    void addBanner(BannerUploadDto bannerDetails);

    List<HomeBannerDto> getAllBanners();

    HomeBanner findByImageId(Integer imageId);

    void deleteImage(Integer imageId);

    void updateBanner(Integer id, String bannerLink, String imageAlt);
}
