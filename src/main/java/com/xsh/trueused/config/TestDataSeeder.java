package com.xsh.trueused.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xsh.trueused.address.repository.AddressRepository;
import com.xsh.trueused.auth.repository.RoleRepository;
import com.xsh.trueused.category.repository.CategoryRepository;
import com.xsh.trueused.chat.repository.ChatMessageRepository;
import com.xsh.trueused.chat.repository.ChatSessionRepository;
import com.xsh.trueused.coupon.repository.CouponRepository;
import com.xsh.trueused.coupon.repository.UserCouponRepository;
import com.xsh.trueused.entity.Address;
import com.xsh.trueused.entity.BrowsingHistory;
import com.xsh.trueused.entity.Category;
import com.xsh.trueused.entity.ChatMessage;
import com.xsh.trueused.entity.ChatSession;
import com.xsh.trueused.entity.Comment;
import com.xsh.trueused.entity.Coupon;
import com.xsh.trueused.entity.Favorite;
import com.xsh.trueused.entity.Order;
import com.xsh.trueused.entity.Product;
import com.xsh.trueused.entity.ProductImage;
import com.xsh.trueused.entity.RefundRequest;
import com.xsh.trueused.entity.Review;
import com.xsh.trueused.entity.Role;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.entity.UserCoupon;
import com.xsh.trueused.entity.Wallet;
import com.xsh.trueused.enums.CouponType;
import com.xsh.trueused.enums.ProductCondition;
import com.xsh.trueused.enums.ProductStatus;
import com.xsh.trueused.enums.ProductTradeModel;
import com.xsh.trueused.enums.RefundStatus;
import com.xsh.trueused.enums.RefundType;
import com.xsh.trueused.enums.RoleName;
import com.xsh.trueused.enums.UserStatus;
import com.xsh.trueused.interaction.repository.BrowsingHistoryRepository;
import com.xsh.trueused.interaction.repository.CommentRepository;
import com.xsh.trueused.interaction.repository.FavoriteRepository;
import com.xsh.trueused.order.enums.OrderStatus;
import com.xsh.trueused.order.repository.OrderRepository;
import com.xsh.trueused.product.mapper.ProductMapper;
import com.xsh.trueused.product.repository.ProductImageRepository;
import com.xsh.trueused.product.repository.ProductRepository;
import com.xsh.trueused.refund.repository.RefundRequestRepository;
import com.xsh.trueused.review.repository.ReviewRepository;
import com.xsh.trueused.user.repository.UserRepository;
import com.xsh.trueused.wallet.repository.WalletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.test-data.enabled", havingValue = "true")
public class TestDataSeeder implements ApplicationRunner {

    private static final String DEFAULT_PASSWORD = "123456";
    private static final Random RANDOM = new Random(20260304L);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final FavoriteRepository favoriteRepository;
    private final BrowsingHistoryRepository browsingHistoryRepository;
    private final CommentRepository commentRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final ReviewRepository reviewRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (userRepository.existsByUsername("seed_admin")) {
            log.info("测试数据已存在，跳过批量初始化。");
            return;
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER).orElseGet(() -> {
            Role role = new Role();
            role.setName(RoleName.ROLE_USER);
            role.setDescription("Seed User Role");
            return roleRepository.save(role);
        });
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseGet(() -> {
            Role role = new Role();
            role.setName(RoleName.ROLE_ADMIN);
            role.setDescription("Seed Admin Role");
            return roleRepository.save(role);
        });

        User admin = buildUser("seed_admin", "seed_admin@trueused.local", "系统管理员", userRole, adminRole);
        userRepository.save(admin);

        List<User> sellers = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            sellers.add(userRepository.save(buildUser(
                    String.format(Locale.ROOT, "seed_seller_%02d", i),
                    String.format(Locale.ROOT, "seed_seller_%02d@trueused.local", i),
                    String.format(Locale.ROOT, "卖家%02d", i),
                    userRole)));
        }

        List<User> buyers = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            buyers.add(userRepository.save(buildUser(
                    String.format(Locale.ROOT, "seed_buyer_%02d", i),
                    String.format(Locale.ROOT, "seed_buyer_%02d@trueused.local", i),
                    String.format(Locale.ROOT, "买家%02d", i),
                    userRole)));
        }

        createWallets(admin, sellers, buyers);
        List<Address> buyerAddresses = createBuyerAddresses(buyers);
        List<Category> leafCategories = resolveLeafCategories();
        List<Product> products = createProducts(sellers, leafCategories);
        createInteractions(products, buyers, sellers);
        createChatData(sellers, buyers);
        Coupon coupon = ensureSeedCoupon();
        grantCouponsToBuyers(coupon, buyers);
        createOrdersAndReviews(products, buyers, buyerAddresses);

        log.info("测试数据初始化完成：users={}, products={}, orders={}",
                userRepository.count(),
                productRepository.count(),
                orderRepository.count());
    }

    private User buildUser(String username, String email, String nickname, Role... roles) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setNickname(nickname);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setStatus(UserStatus.ACTIVE);
        user.setAvatarUrl("seed/avatar/" + username + ".jpg");
        user.setBio("测试账号：" + nickname);
        user.setLocation("上海");
        for (Role role : roles) {
            user.getRoles().add(role);
        }
        return user;
    }

    private void createWallets(User admin, List<User> sellers, List<User> buyers) {
        List<User> allUsers = new ArrayList<>();
        allUsers.add(admin);
        allUsers.addAll(sellers);
        allUsers.addAll(buyers);

        for (User user : allUsers) {
            Wallet wallet = new Wallet();
            wallet.setUser(user);
            if (user.getUsername().startsWith("seed_seller_")) {
                wallet.setBalance(new BigDecimal("1200.00"));
            } else if (user.getUsername().startsWith("seed_buyer_")) {
                wallet.setBalance(new BigDecimal("8000.00"));
            } else {
                wallet.setBalance(new BigDecimal("10000.00"));
            }
            wallet.setPayPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            walletRepository.save(wallet);
        }
    }

    private List<Address> createBuyerAddresses(List<User> buyers) {
        List<Address> addresses = new ArrayList<>();
        for (int i = 0; i < buyers.size(); i++) {
            User buyer = buyers.get(i);
            Address address = new Address();
            address.setUser(buyer);
            address.setRecipientName(buyer.getNickname());
            address.setPhone("1380000" + String.format(Locale.ROOT, "%04d", i + 1));
            address.setProvince("上海市");
            address.setCity("上海市");
            address.setDistrict("浦东新区");
            address.setDetailedAddress("测试街道" + (100 + i) + "号");
            address.setIsDefault(true);
            address.setAreaCode("310115");
            addresses.add(addressRepository.save(address));
        }
        return addresses;
    }

    private List<Category> resolveLeafCategories() {
        List<Category> all = categoryRepository.findAll();
        List<Category> leaf = all.stream().filter(c -> c.getParent() != null).collect(Collectors.toList());
        if (!leaf.isEmpty()) {
            return leaf;
        }

        Category root = new Category();
        root.setName("测试分类");
        root.setSlug("seed-root");
        root.setPath("/seed");
        root.setStatus("ACTIVE");
        root = categoryRepository.save(root);

        Category child = new Category();
        child.setName("测试子分类");
        child.setParent(root);
        child.setSlug("seed-leaf");
        child.setPath("/seed/leaf");
        child.setStatus("ACTIVE");
        return List.of(categoryRepository.save(child));
    }

    private List<Product> createProducts(List<User> sellers, List<Category> categories) {
        List<Product> products = new ArrayList<>();
        int counter = 1;
        for (User seller : sellers) {
            for (int i = 0; i < 6; i++) {
                Product product = new Product();
                product.setSeller(seller);
                product.setTitle(String.format(Locale.ROOT, "测试商品%02d", counter));
                product.setDescription("批量生成测试商品，用于联调与回归测试。");
                BigDecimal price = new BigDecimal(200 + RANDOM.nextInt(1800));
                product.setPrice(price);
                product.setOriginalPrice(price.add(new BigDecimal(100 + RANDOM.nextInt(300))));
                product.setCondition(ProductCondition.values()[RANDOM.nextInt(ProductCondition.values().length)]);
                product.setStatus(ProductStatus.ON_SALE);
                product.setTradeModel(ProductTradeModel.FREE_TRADING);
                product.setCurrency("CNY");
                product.setLocationText("上海");
                product.setViewsCount((long) (100 + RANDOM.nextInt(5000)));
                product.setFavoritesCount((long) RANDOM.nextInt(300));
                product.setCategory(categories.get(counter % categories.size()));
                products.add(productRepository.save(product));

                ProductImage image = new ProductImage();
                image.setProduct(product);
                image.setImageKey("seed/product/" + product.getId() + ".jpg");
                image.setSort(0);
                image.setIsCover(true);
                productImageRepository.save(image);
                product.getImages().add(image);

                counter++;
            }
        }
        return products;
    }

    private void createInteractions(List<Product> products, List<User> buyers, List<User> sellers) {
        for (User buyer : buyers) {
            for (int i = 0; i < 6; i++) {
                Product product = products.get(RANDOM.nextInt(products.size()));

                BrowsingHistory history = new BrowsingHistory();
                history.setUser(buyer);
                history.setProduct(product);
                history.setViewedAt(Instant.now().minus(RANDOM.nextInt(20), ChronoUnit.DAYS));
                browsingHistoryRepository.save(history);

                if (i % 2 == 0 && !favoriteRepository.existsByUserAndProduct(buyer, product)) {
                    Favorite favorite = new Favorite();
                    favorite.setUser(buyer);
                    favorite.setProduct(product);
                    favorite.setNote("测试收藏");
                    favoriteRepository.save(favorite);
                }

                Comment comment = new Comment();
                comment.setUser(buyer);
                comment.setProduct(product);
                comment.setTargetUser(product.getSeller());
                comment.setContent("测试评论：" + product.getTitle());
                commentRepository.save(comment);
            }
        }

        for (User seller : sellers) {
            Comment profileComment = new Comment();
            profileComment.setUser(buyers.get(RANDOM.nextInt(buyers.size())));
            profileComment.setTargetUser(seller);
            profileComment.setContent("卖家服务不错，继续加油。");
            commentRepository.save(profileComment);
        }
    }

    private void createChatData(List<User> sellers, List<User> buyers) {
        for (int i = 0; i < Math.min(sellers.size(), buyers.size()); i++) {
            User seller = sellers.get(i);
            User buyer = buyers.get(i);

            ChatSession session = new ChatSession();
            if (seller.getId() < buyer.getId()) {
                session.setUserA(seller);
                session.setUserB(buyer);
            } else {
                session.setUserA(buyer);
                session.setUserB(seller);
            }
            session.setLastMessageContent("这件商品还在吗？");
            session.setLastMessageTime(Instant.now().minus(i, ChronoUnit.HOURS));
            session = chatSessionRepository.save(session);

            ChatMessage m1 = new ChatMessage();
            m1.setChatSession(session);
            m1.setSender(buyer);
            m1.setReceiver(seller);
            m1.setContent("你好，这个商品还能小刀吗？");
            m1.setRead(true);
            chatMessageRepository.save(m1);

            ChatMessage m2 = new ChatMessage();
            m2.setChatSession(session);
            m2.setSender(seller);
            m2.setReceiver(buyer);
            m2.setContent("可以适当优惠，欢迎下单。");
            m2.setRead(i % 2 == 0);
            chatMessageRepository.save(m2);
        }
    }

    private Coupon ensureSeedCoupon() {
        List<Coupon> activeCoupons = couponRepository.findByIsActiveTrue();
        if (!activeCoupons.isEmpty()) {
            return activeCoupons.get(0);
        }

        Coupon coupon = new Coupon();
        coupon.setCode("SEED_DISCOUNT_20");
        coupon.setTitle("测试满减券");
        coupon.setDescription("批量测试数据专用");
        coupon.setType(CouponType.DISCOUNT);
        coupon.setDiscountAmount(new BigDecimal("20.00"));
        coupon.setMinSpend(new BigDecimal("199.00"));
        coupon.setValidDays(30);
        coupon.setIsActive(true);
        return couponRepository.save(coupon);
    }

    private void grantCouponsToBuyers(Coupon coupon, List<User> buyers) {
        for (User buyer : buyers) {
            UserCoupon userCoupon = new UserCoupon();
            userCoupon.setUser(buyer);
            userCoupon.setCoupon(coupon);
            userCoupon.setIsUsed(false);
            userCoupon.setClaimedAt(Instant.now().minus(1, ChronoUnit.DAYS));
            userCoupon.setValidUntil(Instant.now().plus(30, ChronoUnit.DAYS));
            userCouponRepository.save(userCoupon);
        }
    }

    private void createOrdersAndReviews(List<Product> products, List<User> buyers, List<Address> addresses) throws Exception {
        int cursor = 0;

        for (int i = 0; i < 4; i++) {
            Product product = products.get(cursor++);
            createOrder(product, buyers.get(i), addresses.get(i), OrderStatus.PENDING_PAYMENT, 25, false, false);
            product.setStatus(ProductStatus.LOCKED);
            productRepository.save(product);
        }

        for (int i = 0; i < 4; i++) {
            Product product = products.get(cursor++);
            createOrder(product, buyers.get(i + 1), addresses.get(i + 1), OrderStatus.PAID, 3, true, false);
            product.setStatus(ProductStatus.SOLD);
            productRepository.save(product);
        }

        for (int i = 0; i < 5; i++) {
            Product product = products.get(cursor++);
            createOrder(product, buyers.get(i + 2), addresses.get(i + 2), OrderStatus.SHIPPED, 10 + i, true, false);
            product.setStatus(ProductStatus.SOLD);
            productRepository.save(product);
        }

        for (int i = 0; i < 5; i++) {
            Product product = products.get(cursor++);
            Order completed = createOrder(product, buyers.get(i + 3), addresses.get(i + 3), OrderStatus.COMPLETED, 18 + i, true,
                    true);
            product.setStatus(ProductStatus.SOLD);
            productRepository.save(product);

            Review review = new Review();
            review.setOrder(completed);
            review.setProduct(product);
            review.setBuyer(completed.getBuyer());
            review.setSeller(completed.getSeller());
            review.setRating(4 + (i % 2));
            review.setContent("测试评价：整体满意，物流很快。");
            review.setIsAnonymous(i % 3 == 0);
            reviewRepository.save(review);
        }

        for (int i = 0; i < 3; i++) {
            Product product = products.get(cursor++);
            Order refunding = createOrder(product, buyers.get(i + 1), addresses.get(i + 1), OrderStatus.REFUNDING, 6 + i, true,
                    false);
            product.setStatus(ProductStatus.SOLD);
            productRepository.save(product);

            RefundRequest request = new RefundRequest();
            request.setOrder(refunding);
            request.setReason("测试退款单据");
            request.setRefundType(i == 0 ? RefundType.REFUND_ONLY : RefundType.RETURN_REFUND);
            request.setRefundAmount(refunding.getPrice());
            request.setStatus(i == 2 ? RefundStatus.APPROVED : RefundStatus.PENDING);
            if (i == 2) {
                request.setUpdatedAt(Instant.now().minus(8, ChronoUnit.DAYS));
            }
            refundRequestRepository.save(request);
        }
    }

    private Order createOrder(Product product, User buyer, Address address, OrderStatus status, int daysAgo,
            boolean paid, boolean delivered) throws Exception {
        Order order = new Order();
        order.setProduct(product);
        order.setSeller(product.getSeller());
        order.setBuyer(buyer);
        order.setAddress(address);
        order.setPrice(product.getPrice());
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setStatus(status);

        if (paid) {
            order.setPaymentTime(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
            order.setTransactionId("SEED_TX_" + System.nanoTime());
        }
        if (status == OrderStatus.SHIPPED || status == OrderStatus.COMPLETED || status == OrderStatus.REFUNDING) {
            order.setExpressCompany("顺丰速运");
            order.setExpressCode("SF");
            order.setTrackingNumber("SF" + (100000 + RANDOM.nextInt(899999)));
            order.setShippedAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
            order.setEstimatedDeliveryTime(Instant.now().minus(Math.max(1, daysAgo - 2), ChronoUnit.DAYS));
        }
        if (delivered) {
            order.setDeliveredAt(Instant.now().minus(Math.max(1, daysAgo - 1), ChronoUnit.DAYS));
        }

        try {
            order.setProductSnapshot(objectMapper.writeValueAsString(ProductMapper.toDTO(product)));
        } catch (Exception ex) {
            log.warn("生成商品快照失败，order for product={}", product.getId(), ex);
        }

        return orderRepository.save(order);
    }
}
