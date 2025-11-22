package com.xsh.trueused.dto;

import lombok.Data;

@Data
public class AddressDTO {
    private Long id;
    private String recipientName;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detailedAddress;
    private Boolean isDefault;
    private String areaCode;
}