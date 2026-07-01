package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.common.FileUtils;
import com.mypropertyfact.estate.entities.*;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.repositories.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
public class ProjectExcelUploadService {

    @Value("${upload_dir}")
    private String uploadDir;

    @Autowired
    private FileUtils fileUtils;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private BuilderRepository builderRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private StateRepository stateRepository;
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private ProjectStatusRepository projectStatusRepository;
    @Autowired
    private ProjectTypeRepository projectTypeRepository;
    @Autowired
    private ProjectGalleryRepository projectGalleryRepository;
    @Autowired
    private ProjectAboutRepository projectAboutRepository;
    @Autowired
    private ProjectDesktopBannerRepository projectDesktopBannerRepository;
    @Autowired
    private ProjectMobileBannerRepository projectMobileBannerRepository;
    @Autowired
    private ProjectWalkthroughRepository projectWalkthroughRepository;
    @Autowired
    private FloorPlanRepository floorPlanRepository;
    @Autowired
    private AmenityRepository amenityRepository;
    @Autowired
    private ProjectFaqsRepository projectFaqsRepository;
    @Autowired
    private LocationBenefitRepository locationBenefitRepository;

    private static final int GALLERY_W = 1600;
    private static final int GALLERY_H = 1200;
    private static final int DESKTOP_W = 2508;
    private static final int DESKTOP_H = 1200;
    private static final int MOBILE_W = 800;
    private static final int MOBILE_H = 800;
    private static final int PROJECT_LOGO_WIDTH = 792;
    private static final int PROJECT_LOGO_HEIGHT = 203;
    private static final int PROJECT_LOCATION_WIDTH = 1200;
    private static final int PROJECT_LOCATION_HEIGHT = 800;

    @Transactional(rollbackFor = Exception.class)
    public Response uploadProjectsExcel(MultipartFile excelFile, MultipartFile imagesZip) {
        Response response = new Response();
        if (excelFile == null || excelFile.isEmpty()) {
            response.setIsSuccess(0);
            response.setMessage("Excel file is required");
            return response;
        }
        String filename = excelFile.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            response.setIsSuccess(0);
            response.setMessage("Only Excel files (.xlsx or .xls) are allowed");
            return response;
        }

        Map<String, byte[]> imageMap = new HashMap<>();
        if (imagesZip != null && !imagesZip.isEmpty() && imagesZip.getOriginalFilename() != null
                && imagesZip.getOriginalFilename().toLowerCase().endsWith(".zip")) {
            try {
                extractZipToMap(imagesZip.getInputStream(), imageMap);
            } catch (Exception e) {
                log.warn("Could not extract images zip: {}", e.getMessage());
            }
        }

        List<String> errors = new ArrayList<>();
        int created = 0;
        int updated = 0;

        try (InputStream is = excelFile.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
                response.setIsSuccess(0);
                response.setMessage("Excel must have a header row and at least one data row");
                return response;
            }

            Row headerRow = sheet.getRow(0);
            Map<String, Integer> colIndex = buildHeaderIndex(headerRow);

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null)
                    continue;

                try {
                    String projectName = getCell(colIndex, row, "PROJECT NAME");
                    if (projectName == null || projectName.trim().isEmpty()) {
                        errors.add("Row " + (r + 1) + ": Project name is required");
                        continue;
                    }

                    Project project = resolveOrCreateProject(row, colIndex, projectName.trim(), errors, r + 1);
                    if (project == null)
                        continue;

                    String slug = project.getSlugURL();
                    if (slug == null || slug.isEmpty()) {
                        slug = fileUtils.generateSlug(projectName);
                        project.setSlugURL(slug);
                    }

                    boolean isNew = project.getId() == 0;
                    projectRepository.save(project);

                    // Project walkthrough (text from Excel only)
                    Optional<ProjectWalkthrough> existingWalkthrough =
                            projectWalkthroughRepository.findFirstByProject_IdOrderByIdDesc(project.getId());
                    if (existingWalkthrough.isEmpty()) {
                        String walkthroughDesc = getCell(colIndex, row, "PROJECT WALKTHROUGH DESCRIPTION");
                        if (walkthroughDesc != null && !walkthroughDesc.trim().isEmpty()) {
                            ProjectWalkthrough projectWalkthrough = new ProjectWalkthrough();
                            projectWalkthrough.setWalkthroughDesc(walkthroughDesc.trim());
                            projectWalkthrough.setProject(project);
                            projectWalkthroughRepository.save(projectWalkthrough);
                        }
                    }

                    // About / overview (text from Excel only)
                    String aboutDesc = getCell(colIndex, row,  "PROJECT ABOUT SHORT DESCRIPTION");
                    if (aboutDesc != null && !aboutDesc.trim().isEmpty()) {
                        ProjectsAbout about = projectAboutRepository
                                .findFirstByProject_IdOrderByIdDesc(project.getId()).orElse(null);
                        if (about == null) {
                            about = new ProjectsAbout();
                            about.setProject(project);
                        }
                        about.setLongDesc(aboutDesc.trim());
                        projectAboutRepository.save(about);
                    }

                    // Floor plans from PROJECT CONFIGURATION (e.g. "3 BHK-1333 sq.ft, 4 BHK-1744 sq.ft")
                    String projectConfiguration = getCell(colIndex, row, "PROJECT CONFIGURATION");
                    if (projectConfiguration != null && !projectConfiguration.trim().isEmpty()) {
                        saveFloorPlansFromConfiguration(project, projectConfiguration.trim());
                    }

                    // Project amenities from column (e.g. "Swimming-Pool_Gymnasium_Club-House_Waiting-Lounge") – split by _ and link from amenities table
                    String projectAmenities = getCellWithFallback(colIndex, row, "PROJECT AMENITIES", "PROJECT Amenities");
                    if (projectAmenities != null && !projectAmenities.trim().isEmpty()) {
                        linkAmenitiesFromColumn(project, projectAmenities.trim());
                    }

                    // Location advantages (e.g. "Hyatt-Regency-Gurugram-4.2-Km_Entertainland-Mall-4.4-Km_") – split by _, parse name and distance per segment
                    String locationAdvantage = getCell(colIndex, row, "LOCATION ADVANTAGE");
                    if (locationAdvantage != null && !locationAdvantage.trim().isEmpty()) {
                        saveLocationAdvantagesFromColumn(project, locationAdvantage.trim());
                    }

                    // FAQs from Question 1, Answer 1, Question 2, Answer 2, ...
                    saveFaqsFromRow(project, row, colIndex);

                    if (isNew)
                        created++;
                    else
                        updated++;

                } catch (Exception e) {
                    errors.add("Row " + (r + 1) + ": " + e.getMessage());
                    log.warn("Row {} error: {}", r + 1, e.getMessage());
                }
            }

            // Process images only from zip (filename format: ProjectName-123_ImageType_My-Property-Fact)
            if (!imageMap.isEmpty()) {
                int imagesProcessed = processZipImages(imageMap);
                log.info("Processed {} images from zip", imagesProcessed);
            }

            StringBuilder msg = new StringBuilder();
            msg.append("Upload completed. Created: ").append(created).append(", Updated: ").append(updated);
            if (!errors.isEmpty()) {
                msg.append(". Errors: ").append(errors.size()).append(" - ")
                        .append(String.join("; ", errors.subList(0, Math.min(10, errors.size()))));
                if (errors.size() > 10)
                    msg.append("...");
            }
            response.setIsSuccess(1);
            response.setMessage(msg.toString());

        } catch (Exception e) {
            log.error("Excel upload failed", e);
            response.setIsSuccess(0);
            response.setMessage("Failed to process Excel: " + e.getMessage());
        }

        return response;
    }

    /**
     * Process images from the zip map. Filename format: {@code ProjectName-123_ImageType_My-Property-Fact}
     * First segment = project identifier (with hyphens), second = image type (e.g. Gallery-2, Opening-1), third = ignored.
     * Images are saved under the project folder with unique names and generated alt tags.
     */
    private int processZipImages(Map<String, byte[]> imageMap) {
        int count = 0;
        for (Map.Entry<String, byte[]> entry : imageMap.entrySet()) {
            String filename = entry.getKey();
            byte[] data = entry.getValue();
            if (data == null || data.length == 0)
                continue;

            String baseName = filename;
            if (baseName.contains("/"))
                baseName = baseName.substring(baseName.lastIndexOf('/') + 1);
            int dot = baseName.lastIndexOf('.');
            if (dot > 0)
                baseName = baseName.substring(0, dot);

            String[] parts = baseName.split("_");
            if (parts.length < 2) {
                log.debug("Skipping zip file (expected format ProjectName_ImageType_*): {}", filename);
                continue;
            }

            String projectKey = parts[0].trim();   // e.g. Ozar-96
            String imageType = parts[1].trim();    // e.g. Gallery-2, Opening-1

            if (projectKey.isEmpty() || imageType.isEmpty())
                continue;

            String slugFromZip = fileUtils.generateSlug(projectKey.replace("-", " "));
            Optional<Project> projectOpt = projectRepository.findBySlugURL(slugFromZip);
            if (projectOpt.isEmpty()) {
                log.debug("No project found for slug '{}' (from zip name '{}'), skipping image", slugFromZip, filename);
                continue;
            }

            Project project = projectOpt.get();
            String slug = project.getSlugURL();
            if (slug == null || slug.isEmpty())
                slug = fileUtils.generateSlug(project.getProjectName());
            String projectDir = uploadDir + "properties/" + slug + "/";

            String uniqueBaseName = sanitizeImageType(imageType) + "_" + UUID.randomUUID().toString().substring(0, 8);
            String altTag = generateAltTag(project.getProjectName(), imageType);

            if (imageType.toLowerCase().startsWith("gallery")) {
                boolean alreadyExists = projectGalleryRepository.findBySlugUrl(slug).stream()
                        .anyMatch(g -> altTag.equals(g.getAltTag()));
                if (alreadyExists) continue;
                String saved = fileUtils.saveImageFromBytes(data, uniqueBaseName, projectDir, GALLERY_W, GALLERY_H);
                if (saved != null) {
                    ProjectGallery gallery = new ProjectGallery();
                    gallery.setProject(project);
                    gallery.setImage(saved);
                    gallery.setSlugUrl(slug);
                    gallery.setType("gallery");
                    gallery.setAltTag(altTag);
                    projectGalleryRepository.save(gallery);
                    count++;
                }
            } else if (imageType.toLowerCase().startsWith("desktop")) {
                boolean alreadyExists = projectDesktopBannerRepository.findByProject(project).stream()
                        .anyMatch(b -> altTag.equals(b.getDesktopAltTag()));
                if (alreadyExists) continue;
                String saved = fileUtils.saveImageFromBytes(data, uniqueBaseName, projectDir, DESKTOP_W, DESKTOP_H);
                if (saved != null) {
                    ProjectDesktopBanner banner = new ProjectDesktopBanner();
                    banner.setProject(project);
                    banner.setDesktopImage(saved);
                    banner.setDesktopAltTag(altTag);
                    projectDesktopBannerRepository.save(banner);
                    count++;
                }
            } else if (imageType.toLowerCase().startsWith("mobile")) {
                boolean alreadyExists = projectMobileBannerRepository.findByProject(project).stream()
                        .anyMatch(b -> altTag.equals(b.getMobileAltTag()));
                if (alreadyExists) continue;
                String saved = fileUtils.saveImageFromBytes(data, uniqueBaseName, projectDir, MOBILE_W, MOBILE_H);
                if (saved != null) {
                    ProjectMobileBanner banner = new ProjectMobileBanner();
                    banner.setProject(project);
                    banner.setMobileImage(saved);
                    banner.setMobileAltTag(altTag);
                    projectMobileBannerRepository.save(banner);
                    count++;
                }
            } else if (imageType.toLowerCase().startsWith("logo")) {
                if (project.getProjectLogo() != null && !project.getProjectLogo().isBlank()) continue;
                String saved = fileUtils.saveImageFromBytes(data, uniqueBaseName, projectDir, PROJECT_LOGO_WIDTH, PROJECT_LOGO_HEIGHT);
                if (saved != null) {
                    project.setProjectLogo(saved);
                    projectRepository.save(project);
                    count++;
                }
            } else if (imageType.toLowerCase().startsWith("location")) {
                if (project.getLocationMap() != null && !project.getLocationMap().isBlank()) continue;
                String saved = fileUtils.saveImageFromBytes(data, uniqueBaseName, projectDir, PROJECT_LOCATION_WIDTH, PROJECT_LOCATION_HEIGHT);
                if (saved != null) {
                    project.setLocationMap(saved);
                    projectRepository.save(project);
                    count++;
                }
            } else if (imageType.toLowerCase().startsWith("thumbnail")) {
                if (project.getProjectThumbnail() != null && !project.getProjectThumbnail().isBlank()) continue;
                String saved = fileUtils.saveImageFromBytes(data, uniqueBaseName, projectDir, MOBILE_W, MOBILE_H);
                if (saved != null) {
                    project.setProjectThumbnail(saved);
                    projectRepository.save(project);
                    count++;
                }
            } else {
                // Default: save as gallery (only when not already present)
                boolean alreadyExists = projectGalleryRepository.findBySlugUrl(slug).stream()
                        .anyMatch(g -> altTag.equals(g.getAltTag()));
                if (alreadyExists) continue;
                String saved = fileUtils.saveImageFromBytes(data, uniqueBaseName, projectDir, GALLERY_W, GALLERY_H);
                if (saved != null) {
                    ProjectGallery gallery = new ProjectGallery();
                    gallery.setProject(project);
                    gallery.setImage(saved);
                    gallery.setSlugUrl(slug);
                    gallery.setType("gallery");
                    gallery.setAltTag(altTag);
                    projectGalleryRepository.save(gallery);
                    count++;
                }
            }
        }
        return count;
    }

    private static String sanitizeImageType(String imageType) {
        return imageType.replaceAll("[^a-zA-Z0-9-]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "").toLowerCase();
    }

    private static String generateAltTag(String projectName, String imageType) {
        String humanType = imageType.replace("-", " ").replace("_", " ");
        if (projectName == null || projectName.trim().isEmpty())
            return humanType;
        return projectName.trim() + " - " + humanType;
    }

    private Map<String, Integer> buildHeaderIndex(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        if (headerRow == null)
            return map;
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell c = headerRow.getCell(i);
            String val = getCellValueAsString(c);
            if (val != null && !val.trim().isEmpty()) {
                map.put(val.trim().toUpperCase().replaceAll("\\s+", " "), i);
            }
        }
        return map;
    }

    private String getCell(Map<String, Integer> colIndex, Row row, String headerName) {
        Integer idx = colIndex.get(headerName.toUpperCase().replaceAll("\\s+", " "));
        if (idx == null)
            return null;
        Cell c = row.getCell(idx.intValue());
        return getCellValueAsString(c);
    }

    private String getCellWithFallback(Map<String, Integer> colIndex, Row row, String primaryHeader, String fallbackHeader) {
        String val = getCell(colIndex, row, primaryHeader);
        if (val != null && !val.trim().isEmpty())
            return val;
        return getCell(colIndex, row, fallbackHeader);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null)
            return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double n = cell.getNumericCellValue();
                if (n == Math.floor(n))
                    return String.valueOf((long) n);
                return String.valueOf(n);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue();
                }
            default:
                return null;
        }
    }

    private Project resolveOrCreateProject(Row row, Map<String, Integer> colIndex, String projectName,
                                          List<String> errors, int rowNum) {
        String builderName = getCell(colIndex, row,"BUILDER NAME");
        String cityName = getCell(colIndex, row, "CITY NAME");
        String stateName = getCell(colIndex, row, "STATE NAME");
        String countryName = getCell(colIndex, row, "COUNTRY NAME");
        String projectStatusName = getCell(colIndex, row, "PROJECT STATUS NAME");
        String propertyTypeName = getCell(colIndex, row, "PROPERTY TYPE NAME");

        if (builderName == null || builderName.trim().isEmpty()) {
            errors.add("Row " + rowNum + ": Builder / Project organization is required");
            return null;
        }
        if (cityName == null || cityName.trim().isEmpty()) {
            errors.add("Row " + rowNum + ": City name is required");
            return null;
        }

        Builder builder = findOrCreateBuilder(builderName.trim());
        Country country = findOrCreateCountry(countryName != null ? countryName.trim() : "India");
        State state = findOrCreateState(stateName != null ? stateName.trim() : "N/A", country);
        City city = findOrCreateCity(cityName.trim(), state);
        String statusName = (projectStatusName != null && !projectStatusName.trim().isEmpty())
                ? projectStatusName.trim()
                : "Under Construction";
        ProjectStatus status = projectStatusRepository.findByStatusNameIgnoreCase(statusName)
                .orElseGet(() -> projectStatusRepository.findAll().isEmpty() ? null
                        : projectStatusRepository.findAll().get(0));
        ProjectTypes projectType = null;
        if (propertyTypeName != null && !propertyTypeName.trim().isEmpty()) {
            projectType = projectTypeRepository.findByProjectTypeNameIgnoreCase(propertyTypeName.trim())
                    .orElse(null);
        }
        if (projectType == null && !projectTypeRepository.findAll().isEmpty()) {
            projectType = projectTypeRepository.findAll().get(0);
        }

        Optional<Project> existing = projectRepository.findBySlugURL(fileUtils.generateSlug(projectName));
        if (existing.isPresent()) {
            Project p = existing.get();
            mapRowToProject(p, row, colIndex, builder, city, status, projectType);
            return p;
        }

        Project project = new Project();
        project.setProjectName(projectName);
        project.setSlugURL(fileUtils.generateSlug(projectName));
        project.setStatus(true);
        project.setShowFeaturedProperties(false);
        mapRowToProject(project, row, colIndex, builder, city, status, projectType);
        return project;
    }

    private void mapRowToProject(Project project, Row row, Map<String, Integer> colIndex,
                                 Builder builder, City city, ProjectStatus status, ProjectTypes projectType) {
        project.setBuilder(builder);
        project.setCity(city);
        if (status != null)
            project.setProjectStatus(status);
        if (projectType != null)
            project.setProjectTypes(projectType);
        
        setIfPresent(project::setProjectLocality, getCell(colIndex, row, "PROJECT LOCALITY"));
        setIfPresent(project::setProjectConfiguration, getCell(colIndex, row, "PROJECT CONFIGURATION"));
        setIfPresent(project::setProjectPrice, getCell(colIndex, row, "PROJECT PRICE"));
        setIfPresent(project::setReraNo, getCell(colIndex, row, "RERA NO"));
        setIfPresent(project::setFloorPlanDesc, getCell(colIndex, row, "FLOOR PLAN DESCRIPTION"));
        setIfPresent(project::setLocationDesc, getCell(colIndex, row, "LOCATION DESCRIPTION"));
        setIfPresent(project::setAmenityDesc, getCell(colIndex, row, "AMENITY DESCRIPTION"));
        setIfPresent(project::setMetaTitle, getCell(colIndex, row, "META TITLE"));
        setIfPresent(project::setMetaDescription, getCell(colIndex, row, "META DESCRIPTION"));
        setIfPresent(project::setMetaKeyword, getCell(colIndex, row, "META KEYWORD"));
        setIfPresent(project::setIvrNo, getCell(colIndex, row, "IVR NO"));
        setIfPresent(project::setReraWebsite, getCell(colIndex, row, "RERA WEBSITE"));
    }

    private static void setIfPresent(java.util.function.Consumer<String> setter, String value) {
        if (value != null && !value.trim().isEmpty())
            setter.accept(value.trim());
    }

    /**
     * Parse PROJECT CONFIGURATION string (e.g. "3 BHK-1333 sq.ft, 4 BHK-1744 sq.ft") and save each
     * plan type + area into the floor_plans table. Replaces any existing floor plans for this project.
     */
    private void saveFloorPlansFromConfiguration(Project project, String projectConfiguration) {
        if (projectConfiguration == null || projectConfiguration.trim().isEmpty())
            return;
        floorPlanRepository.deleteAll(floorPlanRepository.findByProject(project));
        Pattern areaPattern = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)\\s*(?:sq\\.?ft|sq\\.?\\s*ft|sqft)?", Pattern.CASE_INSENSITIVE);
        for (String part : projectConfiguration.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty())
                continue;
            int dashIdx = trimmed.indexOf('-');
            if (dashIdx <= 0 || dashIdx == trimmed.length() - 1)
                continue;
            String planType = trimmed.substring(0, dashIdx).trim();
            String areaStr = trimmed.substring(dashIdx + 1).trim();
            if (planType.isEmpty())
                continue;
            Matcher m = areaPattern.matcher(areaStr);
            Double areaSqft = null;
            if (m.find()) {
                try {
                    areaSqft = Double.parseDouble(m.group(1));
                } catch (NumberFormatException ignored) {
                    // keep null
                }
            }
            FloorPlan fp = new FloorPlan();
            fp.setProject(project);
            fp.setPlanType(planType);
            fp.setAreaSqft(areaSqft);
            if (areaSqft != null)
                fp.setAreaSqmt(Math.round(areaSqft * 0.092903 * 100.0) / 100.0);
            floorPlanRepository.save(fp);
        }
    }

    /**
     * Parse PROJECT AMENITIES column (e.g. "Swimming-Pool_Gymnasium_Club-House_Waiting-Lounge"),
     * split by underscore, find each in the amenities table and add to the project. Only existing
     * amenities are linked; names not found are skipped.
     */
    private void linkAmenitiesFromColumn(Project project, String projectAmenities) {
        if (projectAmenities == null || projectAmenities.trim().isEmpty())
            return;
        Set<Amenity> amenities = project.getAmenities();
        if (amenities == null) {
            amenities = new HashSet<>();
            project.setAmenities(amenities);
        }
        for (String part : projectAmenities.split("_")) {
            String name = part.trim().replace("-", " ");
            if (name.isEmpty())
                continue;
            Optional<Amenity> opt = amenityRepository.findByTitleIgnoreCase(name);
            if (opt.isEmpty()) {
                String withSpaces = name.replace("-", " ");
                opt = amenityRepository.findByTitleIgnoreCase(withSpaces);
            }
            opt.ifPresent(amenities::add);
        }
        projectRepository.save(project);
    }

    /**
     * Parse LOCATION ADVANTAGE column (e.g. "Hyatt-Regency-Gurugram-4.2-Km_Entertainland-Mall-4.4-Km_").
     * Split by underscore; each segment is "Name-Distance-Km". Replaces existing location benefits for the project.
     */
    private void saveLocationAdvantagesFromColumn(Project project, String locationAdvantage) {
        if (locationAdvantage == null || locationAdvantage.trim().isEmpty())
            return;
        locationBenefitRepository.deleteAll(locationBenefitRepository.findByProject(project));
        Pattern locationPattern = Pattern.compile("^(.+)-([0-9]+(?:\\.[0-9]+)?)-Km$", Pattern.CASE_INSENSITIVE);
        for (String part : locationAdvantage.split("_")) {
            String segment = part.trim();
            if (segment.isEmpty())
                continue;
            Matcher m = locationPattern.matcher(segment);
            if (!m.matches())
                continue;
            String benefitName = m.group(1).trim().replace("-", " ");
            String distance = m.group(2) + " Km";
            LocationBenefit lb = new LocationBenefit();
            lb.setProject(project);
            lb.setBenefitName(benefitName);
            lb.setDistance(distance);
            locationBenefitRepository.save(lb);
        }
    }

    /**
     * Read Question 1, Answer 1, Question 2, Answer 2, ... from row and save as project FAQs. Replaces existing FAQs for the project.
     */
    private void saveFaqsFromRow(Project project, Row row, Map<String, Integer> colIndex) {
        projectFaqsRepository.deleteAll(projectFaqsRepository.findByProject(project));
        String slug = project.getSlugURL() != null ? project.getSlugURL() : fileUtils.generateSlug(project.getProjectName());
        for (int i = 1; i <= 20; i++) {
            String question = getCell(colIndex, row, "Question " + i);
            String answer = getCell(colIndex, row, "Answer " + i);
            if (question == null && answer == null)
                continue;
            if (question != null) question = question.trim();
            if (answer != null) answer = answer.trim();
            if ((question == null || question.isEmpty()) && (answer == null || answer.isEmpty()))
                continue;
            ProjectFaqs faq = new ProjectFaqs();
            faq.setProject(project);
            faq.setFaqQuestion(question != null ? question : "");
            faq.setFaqAnswer(answer != null ? answer : "");
            faq.setSlugUrl(slug);
            projectFaqsRepository.save(faq);
        }
    }

    private Builder findOrCreateBuilder(String name) {
        return builderRepository.findByBuilderNameIgnoreCase(name)
                .orElseGet(() -> {
                    Builder b = new Builder();
                    b.setBuilderName(name);
                    b.setSlugUrl(fileUtils.generateSlug(name));
                    return builderRepository.save(b);
                });
    }

    private Country findOrCreateCountry(String name) {
        return countryRepository.findByCountryNameIgnoreCase(name)
                .orElseGet(() -> {
                    Country c = new Country();
                    c.setCountryName(name);
                    return countryRepository.save(c);
                });
    }

    private State findOrCreateState(String name, Country country) {
        Optional<State> opt = stateRepository.findByStateName(name);
        if (opt.isPresent())
            return opt.get();
        State s = new State();
        s.setStateName(name);
        s.setCountry(country);
        return stateRepository.save(s);
    }

    private City findOrCreateCity(String name, State state) {
        Optional<City> opt = cityRepository.findByNameIgnoreCase(name);
        if (opt.isPresent())
            return opt.get();
        City c = new City();
        c.setName(name);
        c.setSlugUrl(fileUtils.generateSlug(name));
        c.setState(state);
        return cityRepository.save(c);
    }

    private void extractZipToMap(InputStream zipIs, Map<String, byte[]> out) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(zipIs)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory())
                    continue;
                String name = entry.getName();
                if (name.contains("/"))
                    name = name.substring(name.lastIndexOf('/') + 1);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = zis.read(buf)) > 0)
                    baos.write(buf, 0, n);
                out.put(name, baos.toByteArray());
            }
        }
    }
}
