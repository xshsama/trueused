package com.xsh.trueused.refund.mapper;

import org.springframework.stereotype.Component;

import com.xsh.trueused.entity.RefundRequest;
import com.xsh.trueused.refund.dto.RefundRequestDTO;

@Component
public class RefundRequestMapper {

    public RefundRequestDTO toDTO(RefundRequest refundRequest) {
        if (refundRequest == null) {
            return null;
        }

        RefundRequestDTO dto = new RefundRequestDTO();
        dto.setId(refundRequest.getId());
        dto.setOrderId(refundRequest.getOrder() != null ? refundRequest.getOrder().getId() : null);
        dto.setRefundType(refundRequest.getRefundType());
        dto.setStatus(refundRequest.getStatus());
        dto.setRefundAmount(refundRequest.getRefundAmount());
        dto.setReason(refundRequest.getReason());
        dto.setCreatedAt(refundRequest.getCreatedAt());
        dto.setUpdatedAt(refundRequest.getUpdatedAt());
        return dto;
    }
}
