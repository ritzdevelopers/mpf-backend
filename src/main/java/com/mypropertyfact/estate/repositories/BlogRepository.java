package com.mypropertyfact.estate.repositories;

import com.mypropertyfact.estate.entities.Blog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Integer> {

    // To check if a blog with the given slug exists
    boolean existsBySlugUrl(String slugUrl);

    // To fetch a blog by its slug URL
    Blog findBySlugUrl(String slugUrl);

    @EntityGraph(attributePaths = {"blogCategory", "city"})
    @Query("SELECT b FROM Blog b")
    List<Blog> findAllWithBlogCategory();

    long countByStatus(int status);
}
