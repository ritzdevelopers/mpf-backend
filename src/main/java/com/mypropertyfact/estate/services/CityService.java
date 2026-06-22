package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.ConstantMessages;
import com.mypropertyfact.estate.common.CommonMapper;
import com.mypropertyfact.estate.common.FileUtils;
import com.mypropertyfact.estate.dtos.*;
import com.mypropertyfact.estate.entities.*;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.repositories.CityRepository;
import com.mypropertyfact.estate.repositories.LocalityRepository;
import com.mypropertyfact.estate.repositories.ProjectRepository;
import com.mypropertyfact.estate.repositories.StateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class CityService {
    private final CityRepository cityRepository;
    private final StateRepository stateRepository;
    private final CommonMapper commonMapper;
    private final FileUtils fileUtils;
    private final ProjectRepository projectRepository;
    private final LocalityRepository localityRepository;

    @Value("${upload_dir}")
    private String uploadDir;

    @Transactional
    public List<CityDetailDto> getAllCities() {
        List<CityDetailDto> allCities = cityRepository.findAllCities();
        return allCities.stream().map(city -> {
            List<LocalityShortDto> allLocalitiesOfCity = localityRepository.findAllLocalitiesOfCity(city.getId());
            city.setLocalities(allLocalitiesOfCity);
            return city;
        }).toList();
    }

    public Response postNewCity(CityDto cityDto) {
        return saveCity(cityDto, null);
    }

    public Response saveCityWithImage(CityDto cityDto, MultipartFile monumentImage) {
        return saveCity(cityDto, monumentImage);
    }

    private Response saveCity(CityDto cityDto, MultipartFile monumentImage) {
        try {
            City existingCity = this.cityRepository.findByName(cityDto.getCityName());
            if (existingCity != null && existingCity.getId() != cityDto.getId()) {
                return new Response(0, ConstantMessages.CITY_EXISTS, 0);
            }

            String slug = cityDto.getSlugURL();
            if (slug == null || slug.isBlank()) {
                slug = fileUtils.generateSlug(cityDto.getCityName());
            } else {
                slug = fileUtils.generateSlug(slug);
            }
            cityDto.setSlugURL(slug);

            Optional<State> state = stateRepository.findById(cityDto.getStateId());
            if (cityDto.getId() != 0) {
                Optional<City> savedCity = cityRepository.findById(cityDto.getId());
                if (savedCity.isEmpty()) {
                    return new Response(0, "City not found", 0);
                }
                savedCity.ifPresent(city -> {
                    state.ifPresent(city::setState);
                    commonMapper.mapCityToCityDto(city, cityDto);
                    handleMonumentImageUpload(city, monumentImage);
                    cityRepository.save(city);
                });
                return new Response(1, ConstantMessages.CITY_UPDATED, 0);
            } else {
                City city = new City();
                state.ifPresent(city::setState);
                commonMapper.mapCityToCityDto(city, cityDto);
                if (city.getIsActive() == null) {
                    city.setIsActive(true);
                }
                handleMonumentImageUpload(city, monumentImage);
                cityRepository.save(city);
                return new Response(1, ConstantMessages.CITY_ADDED, 0);
            }
        } catch (IllegalArgumentException ex) {
            return new Response(0, ex.getMessage(), 0);
        }
    }

    private void handleMonumentImageUpload(City city, MultipartFile monumentImage) {
        if (monumentImage == null || monumentImage.isEmpty()) {
            return;
        }
        if (!fileUtils.isTypeImage(monumentImage)) {
            throw new IllegalArgumentException("Only image files are allowed for monument image.");
        }
        String cityUploadDir = Paths.get(uploadDir, "cities").toString();
        String slug = city.getSlugUrl() != null ? city.getSlugUrl() : "city";
        String savedFileName = fileUtils.saveFile(
                monumentImage,
                slug + "-monument-" + System.currentTimeMillis(),
                cityUploadDir,
                1200,
                800,
                0.85f
        );
        if (savedFileName != null && !savedFileName.isBlank()) {
            if (city.getMonumentImage() != null && !city.getMonumentImage().isBlank()) {
                fileUtils.deleteFileFromDestination(city.getMonumentImage(), cityUploadDir);
            }
            city.setMonumentImage(savedFileName);
        }
    }

    public Response deleteCity(int id) {
        Response response = new Response();
        try {
            City city = this.cityRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("City Not Found !"));
            if (city != null) {
                this.cityRepository.deleteById(id);
                response.setIsSuccess(1);
                response.setMessage(ConstantMessages.CITY_DELETED);
            }
        } catch (Exception e) {
            response.setMessage(e.getMessage());
            response.setIsSuccess(0);
        }
        return response;
    }

    @Transactional
    public CityDetailDto getBySlug(String url) {
        CityDetailDto dbCity = this.cityRepository.findCityDetails(url);
        if (dbCity == null) {
            return null;
        }
        if (Boolean.FALSE.equals(dbCity.getIsActive())) {
            return null;
        }
        List<LocalityShortDto> allLocalitiesOfCity = localityRepository.findAllLocalitiesOfCity(dbCity.getId());
        dbCity.setLocalities(allLocalitiesOfCity);
        List<ProjectShortDetails> allProjects = projectRepository.findAllProjects();
        allProjects = allProjects.stream().filter(project -> project.getCitySlug().trim()
                .equals(url.trim())).toList();
        dbCity.setProjectList(allProjects);
        return dbCity;
    }

    public Response addUpdateCity(MultipartFile cityImage, City city) {
        return new Response();
    }
}
