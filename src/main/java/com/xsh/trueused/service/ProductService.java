package com.xsh.trueused.service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xsh.trueused.dto.ProductCreateRequest;
import com.xsh.trueused.dto.ProductDTO;
import com.xsh.trueused.dto.ProductUpdateRequest;
import com.xsh.trueused.entity.Category;
import com.xsh.trueused.entity.Product;
import com.xsh.trueused.entity.ProductImage;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.enums.ProductStatus;
import com.xsh.trueused.mapper.ProductMapper;
import com.xsh.trueused.repository.CategoryRepository;
import com.xsh.trueused.repository.ProductRepository;
import com.xsh.trueused.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProductDTO create(ProductCreateRequest req, Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        Product p = new Product();
        p.setSeller(seller);
        applyCreate(req, p);
        p.setStatus(ProductStatus.AVAILABLE); // 直接发布（后续可调整发布策略）
        Product saved = productRepository.save(p);
        return ProductMapper.toDTO(saved);
    }

    @Transactional
    public ProductDTO update(Long id, ProductUpdateRequest req, Long sellerId) {
        Product p = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!Objects.equals(p.getSeller().getId(), sellerId)) {
            throw new SecurityException("无权修改");
        }
        applyUpdate(req, p);
        return ProductMapper.toDTO(p);
    }

    @Transactional
    public void delete(Long id, Long sellerId) {
        Product p = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!Objects.equals(p.getSeller().getId(), sellerId)) {
            throw new SecurityException("无权删除");
        }
        productRepository.delete(p);
    }

    @Transactional(readOnly = true)
    public Optional<ProductDTO> findOne(Long id) {
        return productRepository.findById(id).map(ProductMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> search(String q, Long categoryId, BigDecimal priceMin, BigDecimal priceMax, String sort,
            int page, int size, Long excludeSellerId) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        Specification<Product> spec = (root, query, cb) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(root.get("status"), ProductStatus.AVAILABLE));
            predicates.add(cb.equal(root.get("isDeleted"), Boolean.FALSE));
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim() + "%";
                predicates.add(cb.like(root.get("title"), pattern));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (priceMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), priceMin));
            }
            if (priceMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), priceMax));
            }
            // 排除当前用户发布的商品
            if (excludeSellerId != null) {
                predicates.add(cb.notEqual(root.get("seller").get("id"), excludeSellerId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return productRepository.findAll(spec, pageable).map(ProductMapper::toDTO);
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "created_asc" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "fav_desc" -> Sort.by(Sort.Direction.DESC, "favoritesCount");
            case "views_desc" -> Sort.by(Sort.Direction.DESC, "viewsCount");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private void applyCreate(ProductCreateRequest req, Product p) {
        p.setTitle(req.title());
        p.setDescription(req.description());
        p.setPrice(req.price());
        if (req.currency() != null)
            p.setCurrency(req.currency());
        p.setCondition(req.condition());
        if (req.categoryId() != null) {
            Category c = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("类目不存在"));
            p.setCategory(c);
        }
        p.setLocationText(req.locationText());
        p.setLat(req.lat());
        p.setLng(req.lng());
        p.getImages().clear();
        if (req.imageUrls() != null) {
            int sort = 0;
            for (String url : req.imageUrls()) {
                ProductImage img = new ProductImage();
                img.setProduct(p);
                img.setUrl(url);
                img.setSort(sort++);
                img.setIsCover(sort == 1); // 第一张为封面
                p.getImages().add(img);
            }
        }
    }

    private void applyUpdate(ProductUpdateRequest req, Product p) {
        if (req.title() != null)
            p.setTitle(req.title());
        if (req.description() != null)
            p.setDescription(req.description());
        if (req.price() != null)
            p.setPrice(req.price());
        if (req.currency() != null)
            p.setCurrency(req.currency());
        if (req.status() != null)
            p.setStatus(req.status());
        if (req.condition() != null)
            p.setCondition(req.condition());
        if (req.categoryId() != null) {
            Category c = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("类目不存在"));
            p.setCategory(c);
        }
        if (req.locationText() != null)
            p.setLocationText(req.locationText());
        if (req.lat() != null)
            p.setLat(req.lat());
        if (req.lng() != null)
            p.setLng(req.lng());
        if (req.imageUrls() != null) {
            p.getImages().clear();
            int sort = 0;
            for (String url : req.imageUrls()) {
                ProductImage img = new ProductImage();
                img.setProduct(p);
                img.setUrl(url);
                img.setSort(sort++);
                img.setIsCover(sort == 1);
                p.getImages().add(img);
            }
        }
    }

    @Transactional
    public ProductDTO publishProduct(Long id, Long sellerId) {
        Product p = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!Objects.equals(p.getSeller().getId(), sellerId)) {
            throw new SecurityException("无权操作");
        }
        p.setStatus(ProductStatus.AVAILABLE);
        return ProductMapper.toDTO(p);
    }

    @Transactional
    public ProductDTO hideProduct(Long id, Long sellerId) {
        Product p = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!Objects.equals(p.getSeller().getId(), sellerId)) {
            throw new SecurityException("无权操作");
        }
        p.setStatus(ProductStatus.HIDDEN);
        return ProductMapper.toDTO(p);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> findMyProducts(Long sellerId, String q, ProductStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Product> spec = (root, query, cb) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(root.get("seller").get("id"), sellerId));
            predicates.add(cb.equal(root.get("isDeleted"), Boolean.FALSE));

            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim() + "%";
                predicates.add(cb.like(root.get("title"), pattern));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return productRepository.findAll(spec, pageable).map(ProductMapper::toDTO);
    }
}
