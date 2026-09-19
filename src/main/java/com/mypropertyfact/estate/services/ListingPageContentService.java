package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.ListingPageContentDto;
import com.mypropertyfact.estate.dtos.ListingPageContentPageResponse;
import com.mypropertyfact.estate.dtos.ListingPageContentSummaryDto;
import com.mypropertyfact.estate.entities.ListingPageContent;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.repositories.ListingPageContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ListingPageContentService {

    private final ListingPageContentRepository listingPageContentRepository;

    public List<ListingPageContentSummaryDto> getAllSummaries() {
        return listingPageContentRepository.findAllSummaries("").stream()
                .map(this::toSummary)
                .toList();
    }

    public ListingPageContentPageResponse getAllContents(int page, int size, String category, String q) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String query = q == null || q.isBlank() ? "" : q.trim();

        List<ListingPageContentSummaryDto> filtered = listingPageContentRepository.findAllSummaries(query)
                .stream()
                .map(this::toSummary)
                .filter(row -> matchesCategory(row.getPageSlug(), category))
                .toList();

        int from = Math.min(safePage * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());
        int totalPages = safeSize == 0 ? 0 : (int) Math.ceil(filtered.size() / (double) safeSize);

        return ListingPageContentPageResponse.builder()
                .content(filtered.subList(from, to))
                .totalElements(filtered.size())
                .totalPages(totalPages)
                .number(safePage)
                .size(safeSize)
                .build();
    }

    public Map<String, Object> getBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            return Map.of();
        }
        return listingPageContentRepository
                .findByPageSlugAndIsActiveTrue(slug.trim().toLowerCase())
                .map(this::toMap)
                .orElse(Map.of());
    }

    public Map<String, Object> getById(int id) {
        return listingPageContentRepository.findById(id)
                .map(this::toMap)
                .orElse(Map.of());
    }

    public Response addUpdateContent(ListingPageContentDto dto) {
        Response response = new Response();
        try {
            if (dto == null || dto.getPageSlug() == null || dto.getPageSlug().trim().isEmpty()) {
                response.setMessage("Page slug is required!");
                return response;
            }

            String normalizedSlug = dto.getPageSlug().trim().toLowerCase();
            ListingPageContent row = null;

            if (dto.getId() > 0) {
                Optional<ListingPageContent> byId = listingPageContentRepository.findById(dto.getId());
                if (byId.isEmpty()) {
                    response.setMessage("Listing page content not found!");
                    return response;
                }
                row = byId.get();
            } else {
                row = listingPageContentRepository.findByPageSlug(normalizedSlug).orElse(null);
                if (row == null) {
                    row = new ListingPageContent();
                    row.setActive(true);
                }
            }

            String title = blankToNull(dto.getPageTitle());
            if (title == null) {
                title = formatSlugTitle(normalizedSlug);
            }

            row.setPageSlug(normalizedSlug);
            row.setPageTitle(title);
            row.setHeading(blankToNull(dto.getHeading()));
            row.setIntro(blankToNull(dto.getIntro()));
            row.setContent(blankToNull(dto.getContent()));
            row.setMetaTitle(blankToNull(dto.getMetaTitle()));
            row.setMetaDescription(blankToNull(dto.getMetaDescription()));
            row.setMetaKeywords(blankToNull(dto.getMetaKeywords()));
            row.setActive(dto.isActive());
            listingPageContentRepository.save(row);

            response.setIsSuccess(1);
            response.setMessage(dto.getId() > 0 || row.getId() > 0
                    ? "Listing page content saved successfully..."
                    : "Listing page content added successfully...");
        } catch (Exception e) {
            response.setMessage(e.getMessage());
        }
        return response;
    }

    public Response deleteContent(int id) {
        Response response = new Response();
        try {
            Optional<ListingPageContent> byId = listingPageContentRepository.findById(id);
            if (byId.isPresent()) {
                listingPageContentRepository.deleteById(id);
                response.setIsSuccess(1);
                response.setMessage("Listing page content deleted successfully...");
            } else {
                response.setMessage("Content already deleted or does not exist");
            }
        } catch (Exception e) {
            response.setMessage(e.getMessage());
        }
        return response;
    }

    private ListingPageContentSummaryDto toSummary(Object[] row) {
        String pageSlug = row[1] != null ? row[1].toString() : "";
        String pageTitle = row[2] != null && !row[2].toString().isBlank()
                ? row[2].toString()
                : formatSlugTitle(pageSlug);
        return ListingPageContentSummaryDto.builder()
                .id(asInt(row[0]))
                .pageSlug(pageSlug)
                .pageTitle(pageTitle)
                .heading(row[3] != null ? row[3].toString() : null)
                .metaTitle(row[4] != null ? row[4].toString() : null)
                .isActive(asBoolean(row[5]))
                .hasContent(asBoolean(row[6]))
                .build();
    }

    private boolean matchesCategory(String slug, String category) {
        if (category == null || category.isBlank() || "all".equalsIgnoreCase(category)) {
            return true;
        }
        return resolveCategory(slug).equalsIgnoreCase(category.trim());
    }

    private String resolveCategory(String slug) {
        String value = slug == null ? "" : slug.toLowerCase(Locale.ROOT);
        if (value.equals("projects/commercial") || value.startsWith("commercial-property-in-")) {
            return "commercial";
        }
        if (value.equals("projects/new-launches") || value.startsWith("new-projects-in-")) {
            return "new-projects";
        }
        if (value.equals("projects/residential") || value.startsWith("apartments-in-")) {
            return "apartments";
        }
        if (value.startsWith("flats-in-")) return "flats";
        if (value.startsWith("offices-and-shop-in-")) return "offices";
        if (value.matches("^\\d+-bhk-.*")) return "bhk";
        if (value.matches("^(shops|office|kiosk|food-court|restaurant|showroom|sco-plots)-in-.*")) {
            return "config";
        }
        return "city";
    }

    private int asInt(Object value) {
        if (value instanceof Number number) return number.intValue();
        return 0;
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.intValue() != 0;
        return false;
    }

    private Map<String, Object> toMap(ListingPageContent row) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", row.getId());
        item.put("pageSlug", row.getPageSlug());
        item.put("pageTitle", row.getPageTitle() != null ? row.getPageTitle() : formatSlugTitle(row.getPageSlug()));
        item.put("heading", row.getHeading());
        item.put("intro", row.getIntro());
        item.put("content", row.getContent());
        item.put("metaTitle", row.getMetaTitle());
        item.put("metaDescription", row.getMetaDescription());
        item.put("metaKeywords", row.getMetaKeywords());
        item.put("isActive", row.isActive());
        item.put("hasContent", hasText(row.getContent()) || hasText(row.getIntro()) || hasText(row.getHeading()));
        item.put("hasMeta", hasText(row.getMetaTitle()) || hasText(row.getMetaDescription()) || hasText(row.getMetaKeywords()));
        return item;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String formatSlugTitle(String slug) {
        if (slug == null || slug.isEmpty()) return "";
        return Arrays.stream(slug.split("-"))
                .filter(part -> !part.isEmpty())
                .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1))
                .reduce((a, b) -> a + " " + b)
                .orElse(slug);
    }
}
