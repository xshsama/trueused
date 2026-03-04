package com.xsh.trueused.category.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xsh.trueused.category.dto.CategoryDTO;
import com.xsh.trueused.category.mapper.CategoryMapper;
import com.xsh.trueused.category.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryDTO> listRoot() {
        return categoryRepository.findByParentIsNull().stream().map(CategoryMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> listAll() {
        return categoryRepository.findAll().stream().map(CategoryMapper::toDTO).collect(Collectors.toList());
    }
}
