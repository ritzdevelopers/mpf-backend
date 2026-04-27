package com.mypropertyfact.estate.controllers;

import com.mypropertyfact.estate.entities.Testimonial;
import com.mypropertyfact.estate.models.Response;
import com.mypropertyfact.estate.services.TestimonialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/testimonial")
@RequiredArgsConstructor
public class TestimonialController {

    private final TestimonialService testimonialService;

    @GetMapping("/get-all")
    public ResponseEntity<List<Testimonial>> getAll() {
        return new ResponseEntity<>(testimonialService.getAll(), HttpStatus.OK);
    }

    @GetMapping("/get-active")
    public ResponseEntity<List<Testimonial>> getActive() {
        return new ResponseEntity<>(testimonialService.getActive(), HttpStatus.OK);
    }

    @PostMapping("/post")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_WEBSITE')")
    public ResponseEntity<Response> addUpdate(@RequestBody Testimonial testimonial) {
        Response response = testimonialService.addUpdate(testimonial);
        return new ResponseEntity<>(response, response.getIsSuccess() == 1 ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("@adminPermissionService.can(authentication, 'MANAGE_WEBSITE')")
    public ResponseEntity<Response> delete(@PathVariable("id") Integer id) {
        Response response = testimonialService.delete(id);
        return new ResponseEntity<>(response, response.getIsSuccess() == 1 ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
    }
}
