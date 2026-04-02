package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.common.FileUtils;
import com.mypropertyfact.estate.dtos.CareerApplicationDto;
import com.mypropertyfact.estate.entities.CareerApplication;
import com.mypropertyfact.estate.interfaces.CareerApplicationService;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.repositories.CareerApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CareerApplicationImpl implements CareerApplicationService {

    private final CareerApplicationRepository careerApplicationRepository;

    private final FileUtils fileUtils;
    private final LeadService leadService;

    @Value("${upload_dir}")
    private String uploadDir;

    @Value("${baseUrl}")
    private String baseUrl;

    @Override
    public Response submitApplication(CareerApplicationDto careerApplicationDto) {
        Response response = new Response();
        String savedFileName = null;
        String fileDestination = null;
        if(careerApplicationDto.getResume() == null) {
            throw new IllegalArgumentException("Resume is required !");
        }else if(!fileUtils.isPdfFile(careerApplicationDto.getResume())){
            throw new IllegalArgumentException("File type should pdf only !");
        }
        if(!fileUtils.checkFileSize(careerApplicationDto.getResume())){
            throw new IllegalArgumentException("File should be less than 5MB !");
        }
        try {
            fileDestination = uploadDir.concat("resume/");
            savedFileName = fileUtils.saveOriginalImage(careerApplicationDto.getResume(), fileDestination);
            CareerApplication careerApplication = new CareerApplication();
            careerApplication.setFirstName(careerApplicationDto.getFirstName());
            careerApplication.setLastName(careerApplicationDto.getLastName());
            careerApplication.setEmailId(careerApplicationDto.getEmailId());
            careerApplication.setPhoneNumber(careerApplicationDto.getPhoneNumber());
            careerApplication.setResume(savedFileName);
            careerApplicationRepository.save(careerApplication);
            try {
                String resumeLink = (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + "resume/" + savedFileName;
                leadService.createLead(
                        (careerApplicationDto.getFirstName() + " " + careerApplicationDto.getLastName()).trim(),
                        careerApplicationDto.getPhoneNumber(),
                        careerApplicationDto.getEmailId(),
                        "Career Application",
                        "career",
                        null,
                        null,
                        resumeLink
                );
            } catch (Exception ignored) {
                // Keep career submission flow unchanged if Telegram fails.
            }
            response.setIsSuccess(1);
            response.setMessage("Your application has been submitted successfully...");
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            if(savedFileName != null) {
                fileUtils.deleteFileFromDestination(savedFileName, fileDestination);
            }
        }
        return response;
    }

    @Override
    public List<CareerApplicationDto> getAllCareerApplication() {
        List<CareerApplication> careerApplications = careerApplicationRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<CareerApplicationDto> careerApplicationDtoList = new ArrayList<>();
        careerApplicationDtoList = careerApplications.stream().map(careerApplication -> {
            CareerApplicationDto dto = new CareerApplicationDto();
            dto.setId(careerApplication.getId());
            dto.setResumeFile(careerApplication.getResume());
            dto.setFirstName(careerApplication.getFirstName());
            dto.setLastName(careerApplication.getLastName());
            dto.setPhoneNumber(careerApplication.getPhoneNumber());
            dto.setEmailId(careerApplication.getEmailId());
            dto.setCreatedAt(careerApplication.getCreatedAt());
            return dto;
        }).toList();
        return careerApplicationDtoList;
    }

    @Override
    public Response deleteCareerApplication(Long id) {
        Response response = new Response();
        String destination = uploadDir.concat("resume/");
        try {
            Optional<CareerApplication> careerApplication = careerApplicationRepository.findById(id);
            careerApplication.ifPresent(application -> {
                boolean b = fileUtils.deleteFileFromDestination(application.getResume(), destination);
                if (b) {
                    careerApplicationRepository.deleteById(id);
                    response.setIsSuccess(1);
                    response.setMessage("Career application has been deleted successfully...");
                } else {
                    throw new IllegalArgumentException("Unable to delete application please try again !");
                }
            });
        }catch (Exception e){
            response.setMessage(e.getMessage());
        }
        return response;
    }
}
