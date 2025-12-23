package com.xsh.trueused.service;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.xsh.trueused.repository.FavoriteRepository;
import com.xsh.trueused.repository.ProductRepository;
import com.xsh.trueused.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final FavoriteRepository favoriteRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_STATIC = "product:static:";
    private static final String KEY_STATUS = "product:status:";
    private static final String KEY_VIEWS = "product:views:";
    private static final String KEY_FAVS = "product:favs:";

    @Transactional
    public ProductDTO create(ProductCreateRequest req, Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        Product p = new Product();
        p.setSeller(seller);
        applyCreate(req, p);

        // 根据交易模式设置初始状态
        if (p.getTradeModel() == com.xsh.trueused.enums.ProductTradeModel.FREE_TRADING) {
            p.setStatus(ProductStatus.ON_SALE); // 自主售卖直接上架
        } else {
            p.setStatus(ProductStatus.PENDING); // 其他模式（如寄售）默认为待入仓/待处理
        }

        Product saved = productRepository.save(p);
        return ProductMapper.enrich(ProductMapper.toDTO(saved));
    }

    @Transactional
    public ProductDTO update(Long id, ProductUpdateRequest req, Long sellerId) {
        Product p = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!Objects.equals(p.getSeller().getId(), sellerId)) {
            throw new SecurityException("无权修改");
        }

        BigDecimal oldPrice = p.getPrice();

        applyUpdate(req, p);

        // Price Drop Notification
        if (req.price() != null && req.price().compareTo(oldPrice) < 0) {
            notifyPriceDrop(p, oldPrice, req.price());
        }

        // Invalidate static cache
        redisTemplate.delete(KEY_STATIC + id);

        return ProductMapper.enrich(ProductMapper.toDTO(p));
    }

    private void notifyPriceDrop(Product p, BigDecimal oldPrice, BigDecimal newPrice) {
        java.util.List<com.xsh.trueused.entity.Favorite> favorites = favoriteRepository.findByProduct(p);
        for (com.xsh.trueused.entity.Favorite fav : favorites) {
            String title = "降价提醒";
            String content = String.format("您收藏的宝贝“%s”降价了！从 ¥%s 降至 ¥%s",
                    p.getTitle(), oldPrice, newPrice);
            notificationService.createNotification(
                    fav.getUser().getId(),
                    title,
                    content,
                    "PRICE_DROP",
                    p.getId());
        }
    }

    @Transactional
    public void polishProduct(Long id, Long sellerId) {
        Product p = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!Objects.equals(p.getSeller().getId(), sellerId)) {
            throw new SecurityException("无权操作");
        }

        // Check if already polished today
        java.time.LocalDate lastUpdateDate = java.time.LocalDate.ofInstant(p.getUpdatedAt(),
                java.time.ZoneId.systemDefault());
        java.time.LocalDate today = java.time.LocalDate.now();

        if (lastUpdateDate.equals(today)) {
            throw new IllegalStateException("每天只能擦亮一次");
        }

        p.setUpdatedAt(java.time.Instant.now());
        productRepository.save(p);

        // Invalidate cache
        redisTemplate.delete(KEY_STATIC + id);
    }

    @Transactional
    public void delete(Long id, Long sellerId) {
        Product p = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!Objects.equals(p.getSeller().getId(), sellerId)) {
            throw new SecurityException("无权删除");
        }
        productRepository.delete(p);

        // Clean up cache
        redisTemplate.delete(KEY_STATIC + id);
        redisTemplate.delete(KEY_STATUS + id);
        redisTemplate.delete(KEY_VIEWS + id);
        redisTemplate.delete(KEY_FAVS + id);
    }

    @Transactional(readOnly = true)
    public Optional<ProductDTO> findOne(Long id) {
        ProductDTO staticDto = null;
        String staticKey = KEY_STATIC + id;

        // 1. Try fetch static info from Redis
        try {
            String json = redisTemplate.opsForValue().get(staticKey);
            if (json != null) {
                staticDto = objectMapper.readValue(json, ProductDTO.class);
            }
        } catch (Exception e) {
            log.error("Failed to read product static cache", e);
        }

        // 2. If missing, fetch from DB
        if (staticDto == null) {
            Optional<Product> pOpt = productRepository.findById(id);
            if (pOpt.isEmpty()) {
                return Optional.empty();
            }
            staticDto = ProductMapper.toDTO(pOpt.get());
            // Save to Redis (TTL 6h)
            try {
                redisTemplate.opsForValue().set(staticKey, objectMapper.writeValueAsString(staticDto), 6,
                        TimeUnit.HOURS);
            } catch (Exception e) {
                log.error("Failed to write product static cache", e);
            }
        }

        // 3. Fetch dynamic data (Status, Views, Favorites)
        ProductStatus status = staticDto.status();
        Long views = staticDto.viewsCount();
        Long favs = staticDto.favoritesCount();

        // Status
        String statusStr = redisTemplate.opsForValue().get(KEY_STATUS + id);
        if (statusStr != null) {
            try {
                status = ProductStatus.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                // ignore
            }
        } else {
            // Init Redis status from DB value (which is in staticDto)
            redisTemplate.opsForValue().set(KEY_STATUS + id, status.name());
        }

        // Views
        String viewsStr = redisTemplate.opsForValue().get(KEY_VIEWS + id);
        if (viewsStr != null) {
            views = Long.parseLong(viewsStr);
        } else {
            if (views == null)
                views = 0L;
            redisTemplate.opsForValue().set(KEY_VIEWS + id, String.valueOf(views));
        }

        // Favs
        String favsStr = redisTemplate.opsForValue().get(KEY_FAVS + id);
        if (favsStr != null) {
            favs = Long.parseLong(favsStr);
        } else {
            if (favs == null)
                favs = 0L;
            redisTemplate.opsForValue().set(KEY_FAVS + id, String.valueOf(favs));
        }

        // 4. Merge and return
        return Optional.of(ProductMapper.enrich(withDynamicData(staticDto, status, views, favs)));
    }

    private ProductDTO withDynamicData(ProductDTO dto, ProductStatus status, Long views, Long favs) {
        return new ProductDTO(
                dto.id(), dto.title(), dto.description(), dto.price(), dto.originalPrice(), dto.heatScore(),
                dto.currency(), status, dto.condition(), dto.tradeModel(), dto.seller(), dto.category(),
                dto.locationText(), dto.lat(), dto.lng(), views, favs, dto.images(), dto.createdAt(), dto.updatedAt());
    }

    @Transactional
    public void incrementViews(Long id) {
        redisTemplate.opsForValue().increment(KEY_VIEWS + id);
        redisTemplate.opsForSet().add("product:views:dirty", String.valueOf(id));
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000) // Sync every minute
    @Transactional
    public void syncViewsToDB() {
        java.util.List<String> dirtyIds = redisTemplate.opsForSet().pop("product:views:dirty", 100);
        if (dirtyIds == null || dirtyIds.isEmpty()) {
            return;
        }

        for (String idStr : dirtyIds) {
            try {
                Long id = Long.valueOf(idStr);
                String viewsStr = redisTemplate.opsForValue().get(KEY_VIEWS + id);
                if (viewsStr != null) {
                    Long views = Long.valueOf(viewsStr);
                    productRepository.findById(id).ifPresent(p -> {
                        p.setViewsCount(views);
                        productRepository.save(p);
                    });
                }
            } catch (Exception e) {
                log.error("Failed to sync views for product " + idStr, e);
            }
        }
    }

    @Transactional
    public void updateProductStatus(Long id, ProductStatus status) {
        Product p = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        p.setStatus(status);
        productRepository.save(p);

        // Update Redis
        redisTemplate.opsForValue().set(KEY_STATUS + id, status.name());
        // Invalidate static cache to be safe
        redisTemplate.delete(KEY_STATIC + id);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> search(String q, Long categoryId, BigDecimal priceMin, BigDecimal priceMax, String sort,
            int page, int size, Long excludeSellerId, Long sellerId, ProductStatus status) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        Specification<Product> spec = (root, query, cb) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(cb.equal(root.get("status"), ProductStatus.ON_SALE));
            }
            predicates.add(cb.equal(root.get("isDeleted"), Boolean.FALSE));
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim() + "%";
                predicates.add(cb.like(root.get("title"), pattern));
            }
            if (categoryId != null) {
                // Fetch category and its children to filter by all related IDs
                Category cat = categoryRepository.findById(categoryId).orElse(null);
                if (cat != null) {
                    java.util.List<Long> ids = new java.util.ArrayList<>();
                    ids.add(categoryId);
                    // Add children IDs
                    if (cat.getChildren() != null) {
                        cat.getChildren().forEach(c -> ids.add(c.getId()));
                    }
                    predicates.add(root.get("category").get("id").in(ids));
                } else {
                    // Category not found, return empty result
                    predicates.add(cb.disjunction());
                }
            }
            if (priceMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), priceMin));
            }
            if (priceMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), priceMax));
            }
            // Filter by specific seller
            if (sellerId != null) {
                predicates.add(cb.equal(root.get("seller").get("id"), sellerId));
            } else if (excludeSellerId != null) {
                // 排除当前用户发布的商品
                predicates.add(cb.notEqual(root.get("seller").get("id"), excludeSellerId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return productRepository.findAll(spec, pageable).map(ProductMapper::toDTO).map(ProductMapper::enrich);
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
        if (req.originalPrice() != null)
            p.setOriginalPrice(req.originalPrice());
        if (req.currency() != null)
            p.setCurrency(req.currency());
        p.setCondition(req.condition());
        if (req.categoryId() != null) {
            Category c = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("类目不存在"));
            p.setCategory(c);
        }
        p.setLocationText(req.locationText());
        p.setShippingPayer(req.shippingPayer());
        p.setTradeTypes(req.tradeTypes());
        if (req.tradeModel() != null) {
            p.setTradeModel(req.tradeModel());
        }
        p.setLat(req.lat());
        p.setLng(req.lng());
        p.getImages().clear();
        if (req.imageKeys() != null) {
            int sort = 0;
            for (String imageKey : req.imageKeys()) {
                ProductImage img = new ProductImage();
                img.setProduct(p);
                img.setImageKey(imageKey);
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
        if (req.originalPrice() != null)
            p.setOriginalPrice(req.originalPrice());
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
        if (req.shippingPayer() != null)
            p.setShippingPayer(req.shippingPayer());
        if (req.tradeTypes() != null)
            p.setTradeTypes(req.tradeTypes());
        if (req.tradeModel() != null)
            p.setTradeModel(req.tradeModel());
        if (req.lat() != null)
            p.setLat(req.lat());
        if (req.lng() != null)
            p.setLng(req.lng());
        if (req.imageKeys() != null) {
            p.getImages().clear();
            int sort = 0;
            for (String imageKey : req.imageKeys()) {
                ProductImage img = new ProductImage();
                img.setProduct(p);
                img.setImageKey(imageKey);
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
        updateProductStatus(id, ProductStatus.ON_SALE);
        return ProductMapper.enrich(ProductMapper.toDTO(p));
    }

    @Transactional
    public ProductDTO hideProduct(Long id, Long sellerId) {
        Product p = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!Objects.equals(p.getSeller().getId(), sellerId)) {
            throw new SecurityException("无权操作");
        }
        updateProductStatus(id, ProductStatus.OFF_SHELF);
        return ProductMapper.enrich(ProductMapper.toDTO(p));
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
        return productRepository.findAll(spec, pageable).map(ProductMapper::toDTO).map(ProductMapper::enrich);
    }
}
