package com.xsh.trueused.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.xsh.trueused.dto.OrderDTO;
import com.xsh.trueused.entity.Order;

@Mapper(uses = { UserMapper.class, ProductMapper.class })
public interface OrderMapper {
    OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);

    OrderDTO toDTO(Order order);
}