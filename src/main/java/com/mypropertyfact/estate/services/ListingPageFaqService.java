package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.dtos.ListingPageFaqBulkDto;
import com.mypropertyfact.estate.dtos.ListingPageFaqDto;
import com.mypropertyfact.estate.entities.ListingPageFaq;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.repositories.ListingPageFaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ListingPageFaqService {

    private final ListingPageFaqRepository listingPageFaqRepository;

    public List<Map<String, Object>> getAllFaqsGrouped() {
        List<ListingPageFaq> allFaqs = listingPageFaqRepository.findAllByOrderByPageSlugAscSortOrderAscIdAsc();
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();

        for (ListingPageFaq faq : allFaqs) {
            String slug = faq.getPageSlug();
            Map<String, Object> pageGroup = grouped.computeIfAbsent(slug, key -> {
                Map<String, Object> obj = new HashMap<>();
                obj.put("pageSlug", key);
                obj.put("pageTitle", faq.getPageTitle() != null ? faq.getPageTitle() : formatSlugTitle(key));
                obj.put("faqs", new ArrayList<Map<String, Object>>());
                return obj;
            });

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> faqList = (List<Map<String, Object>>) pageGroup.get("faqs");

            Map<String, Object> faqItem = new HashMap<>();
            faqItem.put("id", faq.getId());
            faqItem.put("question", faq.getFaqQuestion());
            faqItem.put("answer", faq.getFaqAnswer());
            faqItem.put("sortOrder", faq.getSortOrder());
            faqList.add(faqItem);
        }

        return new ArrayList<>(grouped.values());
    }

    public List<Map<String, Object>> getBySlug(String slug) {
        List<ListingPageFaq> faqs = listingPageFaqRepository.findByPageSlugAndIsActiveTrueOrderBySortOrderAscIdAsc(slug);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ListingPageFaq faq : faqs) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", faq.getId());
            item.put("question", faq.getFaqQuestion());
            item.put("answer", faq.getFaqAnswer());
            item.put("sortOrder", faq.getSortOrder());
            result.add(item);
        }
        return result;
    }

    public Response addUpdateFaq(ListingPageFaqDto dto) {
        Response response = new Response();
        try {
            if (dto == null
                    || dto.getPageSlug() == null
                    || dto.getPageSlug().trim().isEmpty()
                    || dto.getQuestion() == null
                    || dto.getQuestion().trim().isEmpty()
                    || dto.getAnswer() == null
                    || dto.getAnswer().trim().isEmpty()) {
                response.setMessage("Page slug, question, and answer are required!");
                return response;
            }

            String normalizedSlug = dto.getPageSlug().trim().toLowerCase();

            if (dto.getId() > 0) {
                Optional<ListingPageFaq> existing = listingPageFaqRepository.findById(dto.getId());
                if (existing.isEmpty()) {
                    response.setMessage("FAQ not found!");
                    return response;
                }
                ListingPageFaq faq = existing.get();
                faq.setPageSlug(normalizedSlug);
                faq.setPageTitle(dto.getPageTitle());
                faq.setFaqQuestion(dto.getQuestion().trim());
                faq.setFaqAnswer(dto.getAnswer().trim());
                faq.setSortOrder(dto.getSortOrder());
                listingPageFaqRepository.save(faq);
                response.setIsSuccess(1);
                response.setMessage("FAQ updated successfully...");
            } else {
                ListingPageFaq faq = new ListingPageFaq();
                faq.setPageSlug(normalizedSlug);
                faq.setPageTitle(dto.getPageTitle());
                faq.setFaqQuestion(dto.getQuestion().trim());
                faq.setFaqAnswer(dto.getAnswer().trim());
                faq.setSortOrder(dto.getSortOrder());
                faq.setActive(true);
                listingPageFaqRepository.save(faq);
                response.setIsSuccess(1);
                response.setMessage("FAQ added successfully...");
            }
        } catch (Exception e) {
            response.setMessage(e.getMessage());
        }
        return response;
    }

    /**
     * Insert multiple FAQs in one request. Each row may target a different page slug.
     * Invalid rows are skipped; at least one valid row is required.
     */
    public Response bulkAddFaqs(ListingPageFaqBulkDto bulkDto) {
        Response response = new Response();
        try {
            if (bulkDto == null || bulkDto.getFaqs() == null || bulkDto.getFaqs().isEmpty()) {
                response.setMessage("At least one FAQ is required!");
                return response;
            }

            int saved = 0;
            int skipped = 0;
            for (ListingPageFaqDto dto : bulkDto.getFaqs()) {
                if (dto == null
                        || dto.getPageSlug() == null
                        || dto.getPageSlug().trim().isEmpty()
                        || dto.getQuestion() == null
                        || dto.getQuestion().trim().isEmpty()
                        || dto.getAnswer() == null
                        || dto.getAnswer().trim().isEmpty()) {
                    skipped++;
                    continue;
                }

                String normalizedSlug = dto.getPageSlug().trim().toLowerCase();
                String title = dto.getPageTitle();
                if (title == null || title.trim().isEmpty()) {
                    title = formatSlugTitle(normalizedSlug);
                }

                ListingPageFaq faq = new ListingPageFaq();
                faq.setPageSlug(normalizedSlug);
                faq.setPageTitle(title.trim());
                faq.setFaqQuestion(dto.getQuestion().trim());
                faq.setFaqAnswer(dto.getAnswer().trim());
                faq.setSortOrder(dto.getSortOrder());
                faq.setActive(true);
                listingPageFaqRepository.save(faq);
                saved++;
            }

            if (saved == 0) {
                response.setMessage("No valid FAQs to add. Page slug, question, and answer are required for each row.");
                return response;
            }

            response.setIsSuccess(1);
            if (skipped > 0) {
                response.setMessage(saved + " FAQ(s) added successfully. " + skipped + " incomplete row(s) skipped.");
            } else {
                response.setMessage(saved + " FAQ(s) added successfully...");
            }
        } catch (Exception e) {
            response.setMessage(e.getMessage());
        }
        return response;
    }

    public Response deleteFaq(int id) {
        Response response = new Response();
        try {
            Optional<ListingPageFaq> byId = listingPageFaqRepository.findById(id);
            if (byId.isPresent()) {
                listingPageFaqRepository.deleteById(id);
                response.setMessage("FAQ deleted successfully...");
                response.setIsSuccess(1);
            } else {
                response.setMessage("FAQ already deleted or does not exist");
            }
        } catch (Exception e) {
            response.setMessage(e.getMessage());
        }
        return response;
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
