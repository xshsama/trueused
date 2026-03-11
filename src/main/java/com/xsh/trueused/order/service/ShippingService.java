package com.xsh.trueused.order.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.xsh.trueused.order.dto.ShippingInfoDTO;
import com.xsh.trueused.order.dto.ShippingInfoDTO.TrackingEvent;
import com.xsh.trueused.entity.Address;

/**
 * 物流服务 - 模拟快递下单和物流追踪
 */
@Service
public class ShippingService {

    private final Random random = new Random();

    // 模拟的物流数据存储（实际项目中应该存入数据库）
    private final Map<String, ShippingInfoDTO> shippingCache = new ConcurrentHashMap<>();

    // 存储订单的始发和目的城市信息
    private final Map<String, ShippingRoute> routeCache = new ConcurrentHashMap<>();

    // 快递公司配置
    private static final Map<String, String> EXPRESS_COMPANIES = Map.of(
            "顺丰速运", "SF",
            "中通快递", "ZTO",
            "圆通速递", "YTO",
            "韵达快递", "YD",
            "申通快递", "STO",
            "极兔速递", "JT",
            "邮政EMS", "EMS",
            "京东物流", "JD");

    // 中转城市列表（用于模拟中途经过的城市）
    private static final String[] TRANSIT_CITIES = {
            "郑州", "武汉", "长沙", "南昌", "合肥", "济南", "石家庄", "太原"
    };

    // 模拟的物流节点
    private static final String[] TRANSIT_HUBS = {
            "分拨中心", "转运中心", "集散中心", "配送站", "营业部"
    };

    /**
     * 物流路线信息
     */
    private static class ShippingRoute {
        String senderCity;
        String senderDistrict;
        String receiverCity;
        String receiverDistrict;

        ShippingRoute(String senderCity, String senderDistrict, String receiverCity, String receiverDistrict) {
            this.senderCity = senderCity != null ? senderCity : "发货地";
            this.senderDistrict = senderDistrict != null ? senderDistrict : "";
            this.receiverCity = receiverCity != null ? receiverCity : "收货地";
            this.receiverDistrict = receiverDistrict != null ? receiverDistrict : "";
        }
    }

    /**
     * 创建快递订单 - 模拟快递公司下单
     * 
     * @param expressCompany  快递公司名称
     * @param trackingNumber  快递单号（可选）
     * @param shipmentType    发货类型
     * @param senderCity      发件城市
     * @param senderDistrict  发件区域
     * @param receiverAddress 收件地址信息
     * @return 物流信息
     */
    public ShippingInfoDTO createShippingOrder(String expressCompany, String trackingNumber,
            String shipmentType, String senderCity, String senderDistrict, Address receiverAddress) {
        // 生成快递单号
        String finalTrackingNumber = trackingNumber;
        if (finalTrackingNumber == null || finalTrackingNumber.isEmpty()) {
            finalTrackingNumber = generateTrackingNumber(expressCompany);
        }

        String expressCode = EXPRESS_COMPANIES.getOrDefault(expressCompany, "OTHER");
        Instant now = Instant.now();

        // 获取收件城市信息
        String receiverCity = receiverAddress != null ? receiverAddress.getCity() : "收货地";
        String receiverDistrict = receiverAddress != null ? receiverAddress.getDistrict() : "";

        // 保存路线信息
        ShippingRoute route = new ShippingRoute(senderCity, senderDistrict, receiverCity, receiverDistrict);
        routeCache.put(finalTrackingNumber, route);

        // 发件地点显示
        String senderLocation = (senderCity != null ? senderCity : "发货地") +
                (senderDistrict != null ? senderDistrict : "");

        // 创建初始物流轨迹
        List<TrackingEvent> events = new ArrayList<>();
        events.add(TrackingEvent.builder()
                .time(now)
                .description(buildCreatedDescription(shipmentType))
                .location(senderLocation)
                .status("PENDING")
                .build());

        // 预计送达时间（2-5天后）
        int deliveryDays = 2 + random.nextInt(4);
        Instant estimatedDelivery = now.plus(deliveryDays, ChronoUnit.DAYS);

        ShippingInfoDTO shippingInfo = ShippingInfoDTO.builder()
                .shipmentType(shipmentType)
                .trackingNumber(finalTrackingNumber)
                .expressCompany(expressCompany)
                .expressCode(expressCode)
                .shippingStatus("PENDING")
                .shippedAt(now)
                .estimatedDeliveryTime(estimatedDelivery)
                .trackingEvents(events)
                .senderCity(senderCity)
                .senderDistrict(senderDistrict)
                .receiverCity(receiverCity)
                .receiverDistrict(receiverDistrict)
                .build();

        // 缓存物流信息
        shippingCache.put(finalTrackingNumber, shippingInfo);

        return shippingInfo;
    }

    /**
     * 重建物流信息（用于系统重启后恢复数据）
     */
    public ShippingInfoDTO reconstructShippingInfo(String trackingNumber, String expressCompany, Instant shippedAt,
            String shipmentType, String senderCity, String senderDistrict, Address receiverAddress) {

        if (trackingNumber == null || shippedAt == null) {
            return null;
        }

        // 如果缓存中已存在，直接返回
        if (shippingCache.containsKey(trackingNumber)) {
            return getShippingInfo(trackingNumber);
        }

        String expressCode = EXPRESS_COMPANIES.getOrDefault(expressCompany, "OTHER");

        // 获取收件城市信息
        String receiverCity = receiverAddress != null ? receiverAddress.getCity() : "收货地";
        String receiverDistrict = receiverAddress != null ? receiverAddress.getDistrict() : "";

        // 重建路线信息
        ShippingRoute route = new ShippingRoute(senderCity, senderDistrict, receiverCity, receiverDistrict);
        routeCache.put(trackingNumber, route);

        // 发件地点显示
        String senderLocation = (senderCity != null ? senderCity : "发货地") +
                (senderDistrict != null ? senderDistrict : "");

        // 重建初始物流轨迹
        List<TrackingEvent> events = new ArrayList<>();
        events.add(TrackingEvent.builder()
                .time(shippedAt)
                .description(buildCreatedDescription(shipmentType))
                .location(senderLocation)
                .status("PENDING")
                .build());

        // 预计送达时间（基于发货时间推算）
        // 为了保持一致性，这里简单假设为发货后3天
        Instant estimatedDelivery = shippedAt.plus(3, ChronoUnit.DAYS);

        ShippingInfoDTO shippingInfo = ShippingInfoDTO.builder()
                .shipmentType(shipmentType)
                .trackingNumber(trackingNumber)
                .expressCompany(expressCompany)
                .expressCode(expressCode)
                .shippingStatus("PENDING")
                .shippedAt(shippedAt)
                .estimatedDeliveryTime(estimatedDelivery)
                .trackingEvents(events)
                .senderCity(senderCity)
                .senderDistrict(senderDistrict)
                .receiverCity(receiverCity)
                .receiverDistrict(receiverDistrict)
                .build();

        // 放入缓存并立即执行一次模拟进度更新
        shippingCache.put(trackingNumber, shippingInfo);
        ShippingInfoDTO refreshed = simulateShippingProgress(shippingInfo, route);
        shippingCache.put(trackingNumber, refreshed);
        return refreshed;
    }

    /**
     * 获取物流追踪信息
     * 
     * @param trackingNumber 快递单号
     * @return 物流信息（包含模拟的物流轨迹）
     */
    public ShippingInfoDTO getShippingInfo(String trackingNumber) {
        ShippingInfoDTO info = shippingCache.get(trackingNumber);
        if (info == null) {
            return null;
        }

        // 获取路线信息
        ShippingRoute route = routeCache.computeIfAbsent(trackingNumber,
                key -> new ShippingRoute(info.getSenderCity(), info.getSenderDistrict(), info.getReceiverCity(), info.getReceiverDistrict()));

        // 模拟物流进度更新
        ShippingInfoDTO refreshed = simulateShippingProgress(info, route);
        shippingCache.put(trackingNumber, refreshed);
        return refreshed;
    }

    public ShippingInfoDTO refreshShippingInfo(ShippingInfoDTO info) {
        if (info == null) {
            return null;
        }

        ShippingRoute route = new ShippingRoute(
                info.getSenderCity(),
                info.getSenderDistrict(),
                info.getReceiverCity(),
                info.getReceiverDistrict());
        routeCache.put(info.getTrackingNumber(), route);
        shippingCache.put(info.getTrackingNumber(), info);

        ShippingInfoDTO refreshed = simulateShippingProgress(info, route);
        shippingCache.put(info.getTrackingNumber(), refreshed);
        return refreshed;
    }

    /**
     * 模拟物流进度 - 根据时间推进物流状态
     */
    private ShippingInfoDTO simulateShippingProgress(ShippingInfoDTO info, ShippingRoute route) {
        if (info == null || info.getShippedAt() == null) {
            return info;
        }

        Instant now = Instant.now();
        Instant shippedAt = info.getShippedAt();
        long hoursElapsed = ChronoUnit.HOURS.between(shippedAt, now);

        List<TrackingEvent> events = info.getTrackingEvents() == null
                ? new ArrayList<>()
                : new ArrayList<>(info.getTrackingEvents());
        String currentStatus = info.getShippingStatus();

        // 使用真实的始发和目的城市
        String senderCity = route != null ? route.senderCity : "发货地";
        String senderDistrict = route != null ? route.senderDistrict : "";
        String receiverCity = route != null ? route.receiverCity : "收货地";
        String receiverDistrict = route != null ? route.receiverDistrict : "";
        String transitCity = getTransitCity(senderCity, receiverCity);

        // 根据经过的时间模拟物流进度
        if (hoursElapsed >= 2 && events.size() < 2) {
            // 2小时后：已揽收
            events.add(TrackingEvent.builder()
                    .time(shippedAt.plus(2, ChronoUnit.HOURS))
                    .description("快递员已揽收，正在发往【" + senderCity + "分拨中心】")
                    .location(senderCity + senderDistrict + "营业部")
                    .status("PICKED")
                    .build());
            currentStatus = "PICKED";
        }

        if (hoursElapsed >= 8 && events.size() < 3) {
            // 8小时后：到达发件城市分拨中心
            events.add(TrackingEvent.builder()
                    .time(shippedAt.plus(8, ChronoUnit.HOURS))
                    .description("快件已到达【" + senderCity + "分拨中心】，准备发往" + receiverCity)
                    .location(senderCity + "分拨中心")
                    .status("IN_TRANSIT")
                    .build());
            currentStatus = "IN_TRANSIT";
        }

        if (hoursElapsed >= 24 && events.size() < 4) {
            // 24小时后：经过中转城市
            events.add(TrackingEvent.builder()
                    .time(shippedAt.plus(24, ChronoUnit.HOURS))
                    .description("快件已到达【" + transitCity + "转运中心】，正在发往" + receiverCity)
                    .location(transitCity + "转运中心")
                    .status("IN_TRANSIT")
                    .build());
        }

        if (hoursElapsed >= 48 && events.size() < 5) {
            // 48小时后：到达目的城市
            events.add(TrackingEvent.builder()
                    .time(shippedAt.plus(48, ChronoUnit.HOURS))
                    .description("快件已到达【" + receiverCity + "分拨中心】，正在派送中")
                    .location(receiverCity + "分拨中心")
                    .status("DELIVERING")
                    .build());
            currentStatus = "DELIVERING";
        }

        if (hoursElapsed >= 56 && events.size() < 6) {
            // 56小时后：派送中
            events.add(TrackingEvent.builder()
                    .time(shippedAt.plus(56, ChronoUnit.HOURS))
                    .description("快件正在派送中，派送员：李师傅，电话：138****8888")
                    .location(receiverCity + receiverDistrict + "配送站")
                    .status("DELIVERING")
                    .build());
        }

        if (hoursElapsed >= 72 && events.size() < 7) {
            // 72小时后：已签收
            events.add(TrackingEvent.builder()
                    .time(shippedAt.plus(72, ChronoUnit.HOURS))
                    .description("快件已签收，签收人：本人签收。感谢使用" + info.getExpressCompany() + "！")
                    .location(receiverCity + receiverDistrict)
                    .status("DELIVERED")
                    .build());
            currentStatus = "DELIVERED";
        }

        return ShippingInfoDTO.builder()
                .shipmentType(info.getShipmentType())
                .trackingNumber(info.getTrackingNumber())
                .expressCompany(info.getExpressCompany())
                .expressCode(info.getExpressCode())
                .shippingStatus(currentStatus)
                .shippedAt(info.getShippedAt())
                .estimatedDeliveryTime(info.getEstimatedDeliveryTime())
                .trackingEvents(events)
                .senderCity(senderCity)
                .senderDistrict(senderDistrict)
                .receiverCity(receiverCity)
                .receiverDistrict(receiverDistrict)
                .build();
    }

    private String buildCreatedDescription(String shipmentType) {
        if ("PLATFORM_OUTBOUND".equals(shipmentType)) {
            return "平台仓已出库，快递员正在揽收中";
        }
        return "卖家已发货，快递员正在揽收中";
    }

    /**
     * 生成快递单号
     */
    private String generateTrackingNumber(String expressCompany) {
        String prefix = EXPRESS_COMPANIES.getOrDefault(expressCompany, "TU");
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(3);
        String randomPart = String.format("%04d", random.nextInt(10000));
        return prefix + timestamp + randomPart;
    }

    /**
     * 根据始发和目的城市选择中转城市
     */
    private String getTransitCity(String senderCity, String receiverCity) {
        // 避免中转城市与始发或目的城市相同
        for (String city : TRANSIT_CITIES) {
            if (!city.equals(senderCity) && !city.equals(receiverCity)) {
                return city;
            }
        }
        return TRANSIT_CITIES[0];
    }

    /**
     * 获取支持的快递公司列表
     */
    public List<String> getSupportedExpressCompanies() {
        return new ArrayList<>(EXPRESS_COMPANIES.keySet());
    }
}
