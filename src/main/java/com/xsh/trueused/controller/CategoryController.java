package com.xsh.trueused.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xsh.trueused.dto.CategoryDTO;
import com.xsh.trueused.service.CategoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryDTO> listAll() {
        return categoryService.listAll();
    }

    @GetMapping("/root")
    public List<CategoryDTO> listRoot() {
        return categoryService.listRoot();
    }
}
