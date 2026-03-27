package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.entities.LocationBenefit;
import com.mypropertyfact.estate.entities.Project;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.repositories.LocationBenefitRepository;
import com.mypropertyfact.estate.repositories.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uploads nearby benefits (location benefits) from Excel for projects that do not yet have any.
 * Excel structure: S.no, Project, School, Malls/ IT Park, Hospitals, Roads/ Highway,
 * Famous for/ Metro, Airport/Famous places.
 * Cell format: Place-Name_Distance-Km (e.g. GD-Goenka-International-School_7-Km).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectNearbyBenefitsExcelService {

    private static final Pattern BENEFIT_PATTERN = Pattern.compile("^(.+)_([0-9]+(?:\\.[0-9]+)?)-Km$", Pattern.CASE_INSENSITIVE);

    private static final List<String> BENEFIT_HEADERS = List.of(
            "SCHOOL",
            "MALLS/ IT PARK",
            "HOSPITALS",
            "ROADS/ HIGHWAY",
            "FAMOUS FOR/ METRO",
            "AIRPORT/FAMOUS PLACES"
    );

    private final ProjectRepository projectRepository;
    private final LocationBenefitRepository locationBenefitRepository;

    @Transactional(rollbackFor = Exception.class)
    public Response uploadNearbyBenefitsExcel(MultipartFile file) {
        Response response = new Response();
        if (file == null || file.isEmpty()) {
            response.setIsSuccess(0);
            response.setMessage("Excel file is required");
            return response;
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            response.setIsSuccess(0);
            response.setMessage("Only Excel files (.xlsx or .xls) are allowed");
            return response;
        }

        List<String> errors = new ArrayList<>();
        int updated = 0;
        int skippedNoProject = 0;
        int skippedHasBenefits = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
                response.setIsSuccess(0);
                response.setMessage("Excel must have a header row and at least one data row");
                return response;
            }

            Row headerRow = sheet.getRow(0);
            Map<String, Integer> colIndex = buildHeaderIndex(headerRow);

            Integer projectCol = colIndex.get("PROJECT");
            if (projectCol == null) {
                response.setIsSuccess(0);
                response.setMessage("Excel must contain a 'Project' column");
                return response;
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String projectName = getCellString(colIndex, row, "PROJECT");
                if (projectName == null || projectName.trim().isEmpty()) {
                    errors.add("Row " + (r + 1) + ": Project name is required");
                    continue;
                }
                projectName = projectName.trim();

                Optional<Project> projectOpt = projectRepository.findFirstByProjectNameIgnoreCase(projectName);
                if (projectOpt.isEmpty()) {
                    errors.add("Row " + (r + 1) + ": Project not found: " + projectName);
                    skippedNoProject++;
                    continue;
                }
                Project project = projectOpt.get();

                List<LocationBenefit> existing = locationBenefitRepository.findByProject(project);
                if (!existing.isEmpty()) {
                    log.debug("Project '{}' already has {} location benefits; skipping", projectName, existing.size());
                    skippedHasBenefits++;
                    continue;
                }

                int benefitsAdded = 0;
                for (String header : BENEFIT_HEADERS) {
                    String cellValue = getCellString(colIndex, row, header);
                    if (cellValue == null || cellValue.trim().isEmpty()) continue;

                    Optional<LocationBenefit> parsed = parseBenefitCell(cellValue.trim());
                    if (parsed.isEmpty()) {
                        errors.add("Row " + (r + 1) + ", column '" + header + "': invalid format (expected Place-Name_Distance-Km)");
                        continue;
                    }
                    LocationBenefit lb = parsed.get();
                    lb.setProject(project);
                    locationBenefitRepository.save(lb);
                    benefitsAdded++;
                }

                if (benefitsAdded > 0) {
                    updated++;
                    log.info("Added {} nearby benefits for project '{}'", benefitsAdded, projectName);
                }
            }

            response.setIsSuccess(1);
            String message = String.format("Processed successfully. Updated: %d project(s); skipped (no project): %d; skipped (already has benefits): %d.",
                    updated, skippedNoProject, skippedHasBenefits);
            if (!errors.isEmpty()) {
                message += " Errors: " + String.join("; ", errors);
            }
            response.setMessage(message);

        } catch (Exception e) {
            log.error("Error uploading nearby benefits Excel", e);
            response.setIsSuccess(0);
            response.setMessage("Upload failed: " + e.getMessage());
        }
        return response;
    }

    private Map<String, Integer> buildHeaderIndex(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        if (headerRow == null) return map;
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell c = headerRow.getCell(i);
            String val = getCellValueAsString(c);
            if (val != null && !val.trim().isEmpty()) {
                map.put(val.trim().toUpperCase().replaceAll("\\s+", " "), i);
            }
        }
        return map;
    }

    private String getCellString(Map<String, Integer> colIndex, Row row, String headerName) {
        Integer idx = colIndex.get(headerName.toUpperCase().replaceAll("\\s+", " "));
        if (idx == null) return null;
        Cell c = row.getCell(idx);
        return getCellValueAsString(c);
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                }
                double n = cell.getNumericCellValue();
                yield (n == Math.floor(n)) ? String.valueOf((long) n) : String.valueOf(n);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    yield cell.toString();
                }
            }
            default -> null;
        };
    }

    /**
     * Parse a cell value in format Place-Name_Distance-Km (e.g. GD-Goenka-International-School_7-Km)
     * into a LocationBenefit with benefitName (hyphens replaced by spaces) and distance "X Km".
     */
    private Optional<LocationBenefit> parseBenefitCell(String value) {
        Matcher m = BENEFIT_PATTERN.matcher(value);
        if (!m.matches()) return Optional.empty();
        String namePart = m.group(1).trim().replace("-", " ");
        String distanceNum = m.group(2);
        String distance = distanceNum + " Km";
        LocationBenefit lb = new LocationBenefit();
        lb.setBenefitName(namePart);
        lb.setDistance(distance);
        return Optional.of(lb);
    }
}
