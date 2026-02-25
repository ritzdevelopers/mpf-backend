package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.ConstantMessages;
import com.mypropertyfact.estate.common.CommonMapper;
import com.mypropertyfact.estate.common.FileUtils;
import com.mypropertyfact.estate.dtos.*;
import com.mypropertyfact.estate.entities.*;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.repositories.CityRepository;
import com.mypropertyfact.estate.repositories.ProjectRepository;
import com.mypropertyfact.estate.repositories.StateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

//    public List<CityView> getAllCities() {
//        return cityRepository.findAllProjectedBy(Sort.by(Sort.Direction.ASC, "name"));
//    }

    @Transactional
    public List<CityDetailDto> getAllCities() {
        return cityRepository.findAllCities();
    }

    public Response postNewCity(CityDto cityDto) {
        City existingCity = this.cityRepository.findByName(cityDto.getCityName());
        if (existingCity != null && existingCity.getId() != cityDto.getId()) {
            return new Response(0, ConstantMessages.CITY_EXISTS, 0);
        }
        cityDto.setSlugURL(fileUtils.generateSlug(cityDto.getCityName()));
        Optional<State> state = stateRepository.findById(cityDto.getStateId());
        if (cityDto.getId() != 0) {
            Optional<City> savedCity = cityRepository.findById(cityDto.getDistrictId());
            savedCity.ifPresent(city -> {
                state.ifPresent(city::setState);
                commonMapper.mapCityToCityDto(city, cityDto);
                cityRepository.save(city);
            });
            return new Response(1, ConstantMessages.CITY_UPDATED, 0);
        } else {
            City city = new City();
            state.ifPresent(city::setState);
            commonMapper.mapCityToCityDto(city, cityDto);
            cityRepository.save(city);
            return new Response(1, ConstantMessages.CITY_ADDED, 0);
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
        List<ProjectShortDetails> allProjects = projectRepository.findAllProjects();
        allProjects = allProjects.stream().filter(project -> project.getCityName().toLowerCase().trim().equals(url.toLowerCase().trim())).toList();
        dbCity.setProjectList(allProjects);
        return dbCity;
    }

    public Response addUpdateCity(MultipartFile cityImage, City city) {
        return new Response();
    }
}
