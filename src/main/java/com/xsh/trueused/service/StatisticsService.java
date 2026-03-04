package com.xsh.trueused.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.xsh.trueused.dto.StatisticsDTO;
import com.xsh.trueused.entity.BrowsingHistory;
import com.xsh.trueused.entity.Order;
import com.xsh.trueused.entity.Product;
import com.xsh.trueused.enums.OrderStatus;
import com.xsh.trueused.enums.ProductStatus;
import com.xsh.trueused.repository.BrowsingHistoryRepository;
import com.xsh.trueused.repository.OrderRepository;
import com.xsh.trueused.repository.ProductRepository;
import com.xsh.trueused.util.CloudinaryUrlHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final OrderRepository orderRepository;
    private final BrowsingHistoryRepository browsingHistoryRepository;
    private final ProductRepository productRepository;

    public StatisticsDTO getSellerStatistics(Long sellerId, String timeRange) {
        StatisticsDTO dto = new StatisticsDTO();

        Instant end = Instant.now();
        Instant start;
        Instant prevEnd;
        Instant prevStart;
        
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zoneId);

        if ("昨日".equals(timeRange)) {
            start = today.minusDays(1).atStartOfDay(zoneId).toInstant();
            end = today.atStartOfDay(zoneId).toInstant().minusMillis(1);
            
            prevStart = today.minusDays(2).atStartOfDay(zoneId).toInstant();
            prevEnd = today.minusDays(1).atStartOfDay(zoneId).toInstant().minusMillis(1);
        } else if ("近30日".equals(timeRange)) {
             start = today.minusDays(29).atStartOfDay(zoneId).toInstant();
             prevStart = start.minus(30, ChronoUnit.DAYS);
             prevEnd = start.minusMillis(1);
        } else if ("近7日".equals(timeRange)) {
             start = today.minusDays(6).atStartOfDay(zoneId).toInstant();
             prevStart = start.minus(7, ChronoUnit.DAYS);
             prevEnd = start.minusMillis(1);
        } else {
             // Default "今日"
             start = today.atStartOfDay(zoneId).toInstant();
             prevStart = today.minusDays(1).atStartOfDay(zoneId).toInstant();
             prevEnd = today.atStartOfDay(zoneId).toInstant().minusMillis(1);
        }

        // Include all orders that are somewhat confirmed
        List<OrderStatus> validStatuses = Arrays.asList(OrderStatus.COMPLETED, OrderStatus.SHIPPED, OrderStatus.PAID);
        // Exclude off-shelf products from seller data center statistics
        List<ProductStatus> visibleProductStatuses = Arrays.asList(
                ProductStatus.ON_SALE,
                ProductStatus.LOCKED,
                ProductStatus.SOLD);

        // Current Period Stats
        BigDecimal currentGmv = orderRepository
                .sumPriceBySellerAndStatusInAndProductStatusInAndCreatedAtBetween(
                        sellerId, validStatuses, visibleProductStatuses, start, end);
        Long currentOrders = orderRepository
                .countBySellerAndStatusInAndProductStatusInAndCreatedAtBetween(
                        sellerId, validStatuses, visibleProductStatuses, start, end);
        Long currentVisitors = browsingHistoryRepository
                .countDistinctUserByProductSellerAndViewedAtBetweenAndProductStatusIn(
                        sellerId, start, end, visibleProductStatuses);

        dto.setGmv(currentGmv != null ? currentGmv : BigDecimal.ZERO);
        dto.setOrderCount(currentOrders != null ? currentOrders : 0L);
        dto.setVisitorCount(currentVisitors != null ? currentVisitors : 0L);

        // Previous Period Stats
        BigDecimal prevGmv = orderRepository
                .sumPriceBySellerAndStatusInAndProductStatusInAndCreatedAtBetween(
                        sellerId, validStatuses, visibleProductStatuses, prevStart, prevEnd);
        Long prevOrders = orderRepository
                .countBySellerAndStatusInAndProductStatusInAndCreatedAtBetween(
                        sellerId, validStatuses, visibleProductStatuses, prevStart, prevEnd);
        Long prevVisitors = browsingHistoryRepository
                .countDistinctUserByProductSellerAndViewedAtBetweenAndProductStatusIn(
                        sellerId, prevStart, prevEnd, visibleProductStatuses);

        if (prevGmv == null) prevGmv = BigDecimal.ZERO;
        if (prevOrders == null) prevOrders = 0L;
        if (prevVisitors == null) prevVisitors = 0L;

        // Growth
        dto.setGmvGrowth(calculateGrowth(dto.getGmv(), prevGmv));
        dto.setOrderGrowth(calculateGrowth(BigDecimal.valueOf(dto.getOrderCount()), BigDecimal.valueOf(prevOrders)));
        dto.setVisitorGrowth(calculateGrowth(BigDecimal.valueOf(dto.getVisitorCount()), BigDecimal.valueOf(prevVisitors)));

        // Trend Data
        dto.setTrend(generateTrend(sellerId, validStatuses, visibleProductStatuses, start, end, zoneId));

        // Top Products
        List<Product> topProducts = productRepository
                .findTop5BySellerIdAndStatusInOrderByViewsCountDesc(
                        sellerId,
                        java.util.Collections.singletonList(ProductStatus.ON_SALE));
        dto.setTopProducts(topProducts.stream().map(p -> {
            StatisticsDTO.ProductRank rank = new StatisticsDTO.ProductRank();
            rank.setId(p.getId());
            rank.setTitle(p.getTitle());
            rank.setViews(p.getViewsCount());
            rank.setPrice(p.getPrice());
            // Mock stock for now (assuming 1 if status is ON_SALE)
            rank.setStock("ON_SALE".equals(p.getStatus().name()) ? 1 : 0);
            if (!p.getImages().isEmpty()) {
                rank.setImage(CloudinaryUrlHelper.getThumbnailUrl(p.getImages().get(0).getImageKey()));
            } else {
                rank.setImage("https://via.placeholder.com/100");
            }
            return rank;
        }).collect(Collectors.toList()));

        return dto;
    }

    private Double calculateGrowth(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(previous).divide(previous, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).doubleValue();
    }

    private List<StatisticsDTO.DailyStat> generateTrend(Long sellerId, List<OrderStatus> statuses,
            List<ProductStatus> productStatuses, Instant start, Instant end, ZoneId zoneId) {
        List<Order> orders = orderRepository
                .findBySellerIdAndStatusInAndProductStatusInAndCreatedAtBetween(
                        sellerId, statuses, productStatuses, start, end);
        List<BrowsingHistory> history = browsingHistoryRepository
                .findByProductSellerIdAndViewedAtBetweenAndProductStatusIn(
                        sellerId, start, end, productStatuses);

        boolean isDaily = Duration.between(start, end).toDays() > 1;

        if (isDaily) {
            Map<LocalDate, BigDecimal> gmvMap = orders.stream()
                .collect(Collectors.groupingBy(
                    o -> o.getCreatedAt().atZone(zoneId).toLocalDate(),
                    Collectors.reducing(BigDecimal.ZERO, Order::getPrice, BigDecimal::add)
                ));
            
            Map<LocalDate, Long> uvMap = history.stream()
                 .collect(Collectors.groupingBy(
                     h -> h.getViewedAt().atZone(zoneId).toLocalDate(),
                     Collectors.collectingAndThen(
                         Collectors.mapping(h -> h.getUser().getId(), Collectors.toSet()),
                         set -> (long) set.size()
                     )
                 ));

            List<StatisticsDTO.DailyStat> dailyStats = new ArrayList<>();
            LocalDate current = start.atZone(zoneId).toLocalDate();
            LocalDate endDate = end.atZone(zoneId).toLocalDate();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

            while (!current.isAfter(endDate)) {
                StatisticsDTO.DailyStat stat = new StatisticsDTO.DailyStat();
                stat.setDate(current.format(formatter));
                stat.setGmv(gmvMap.getOrDefault(current, BigDecimal.ZERO));
                stat.setVisitors(uvMap.getOrDefault(current, 0L));
                dailyStats.add(stat);
                current = current.plusDays(1);
            }
            return dailyStats;
        } else {
            // Hourly
            Map<Integer, BigDecimal> gmvMap = orders.stream()
                .collect(Collectors.groupingBy(
                    o -> o.getCreatedAt().atZone(zoneId).getHour(),
                    Collectors.reducing(BigDecimal.ZERO, Order::getPrice, BigDecimal::add)
                ));
            
            Map<Integer, Long> uvMap = history.stream()
                 .collect(Collectors.groupingBy(
                     h -> h.getViewedAt().atZone(zoneId).getHour(),
                     Collectors.collectingAndThen(
                         Collectors.mapping(h -> h.getUser().getId(), Collectors.toSet()),
                         set -> (long) set.size()
                     )
                 ));

            List<StatisticsDTO.DailyStat> hourlyStats = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                // If today, only go up to current hour? No, display full 24h is cleaner or up to now. 
                // Let's show all 24h
                StatisticsDTO.DailyStat stat = new StatisticsDTO.DailyStat();
                stat.setDate(String.format("%02d:00", i));
                stat.setGmv(gmvMap.getOrDefault(i, BigDecimal.ZERO));
                stat.setVisitors(uvMap.getOrDefault(i, 0L));
                hourlyStats.add(stat);
            }
            return hourlyStats;
        }
    }
}
