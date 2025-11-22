package com.xsh.trueused.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import com.xsh.trueused.dto.AddressDTO;
import com.xsh.trueused.entity.Address;

@Mapper
public interface AddressMapper {
    AddressMapper INSTANCE = Mappers.getMapper(AddressMapper.class);

    AddressDTO toDTO(Address address);
}