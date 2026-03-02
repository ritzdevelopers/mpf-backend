package com.mypropertyfact.estate.dtos;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class BannerUploadDto {
    private HomeBannerDto banner;
    private List<MultipartFile> desktopImages;
    private List<MultipartFile> mobileImages;
    private List<MultipartFile> tabletImages;
}
