package com.xsh.trueused.statistics.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class StatisticsDTO {
    // Overview
    private BigDecimal gmv;
    private Long orderCount;
    private Long visitorCount;
    
    // Growth rates (vs previous period)
    private Double gmvGrowth;
    private Double orderGrowth;
    private Double visitorGrowth;

    // Trend Data
    private List<DailyStat> trend;

    // Top Products
    private List<ProductRank> topProducts;

    @Data
    public static class DailyStat {
        private String date;
        private BigDecimal gmv;
        private Long visitors;
    }

    @Data
    public static class ProductRank {
        private Long id;
        private String title;
        private String image;
        private Long views;
        private Integer stock; // Assuming 1 or 0 for now as it's C2C often
        private BigDecimal price;
    }
}
