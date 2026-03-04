package com.xsh.trueused.mapper;

import com.xsh.trueused.dto.AddressDTO;
import com.xsh.trueused.entity.Address;

public final class AddressMapper {
    public static final AddressMapper INSTANCE = new AddressMapper();

    private AddressMapper() {
    }

    public AddressDTO toDTO(Address address) {
        if (address == null) {
            return null;
        }

        AddressDTO dto = new AddressDTO();
        dto.setId(address.getId());
        dto.setRecipientName(address.getRecipientName());
        dto.setPhone(address.getPhone());
        dto.setProvince(address.getProvince());
        dto.setCity(address.getCity());
        dto.setDistrict(address.getDistrict());
        dto.setDetailedAddress(address.getDetailedAddress());
        dto.setIsDefault(address.getIsDefault());
        dto.setAreaCode(address.getAreaCode());
        return dto;
    }
}
