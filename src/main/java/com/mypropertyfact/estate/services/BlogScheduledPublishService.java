package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.entities.Blog;
import com.mypropertyfact.estate.models.BlogStatus;
import com.mypropertyfact.estate.repositories.BlogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlogScheduledPublishService {

    private final BlogRepository blogRepository;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void publishDueScheduledBlogs() {
        LocalDateTime now = LocalDateTime.now();
        List<Blog> dueBlogs = blogRepository
                .findByStatusAndScheduledPublishAtLessThanEqual(BlogStatus.SCHEDULED, now);

        if (dueBlogs.isEmpty()) {
            return;
        }

        for (Blog blog : dueBlogs) {
            blog.setStatus(BlogStatus.PUBLISHED);
            blog.setScheduledPublishAt(null);
        }
        blogRepository.saveAll(dueBlogs);

        log.info("Auto-published {} scheduled blog(s)", dueBlogs.size());
    }
}
