package com.xsh.trueused.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.xsh.trueused.entity.Address;
import com.xsh.trueused.entity.User;
import com.xsh.trueused.repository.AddressRepository;
import com.xsh.trueused.security.user.UserPrincipal;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressRepository addressRepository;

    public AddressController(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @GetMapping
    public List<java.util.Map<String, Object>> getUserAddresses(@AuthenticationPrincipal UserPrincipal principal) {
        List<Address> addresses = addressRepository.findByUserId(principal.getId());
        return addresses.stream().map(address -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", address.getId());
            map.put("recipientName", address.getRecipientName());
            map.put("phone", address.getPhone());
            map.put("province", address.getProvince());
            map.put("city", address.getCity());
            map.put("district", address.getDistrict());
            map.put("detailedAddress", address.getDetailedAddress());
            map.put("isDefault", address.getIsDefault());
            map.put("areaCode", address.getAreaCode());
            return map;
        }).collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/{id}")
    public Address getAddressById(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!address.getUser().getId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return address;
    }

    @PostMapping
    public Address createAddress(@AuthenticationPrincipal UserPrincipal principal, @RequestBody Address address) {
        User user = new User();
        user.setId(principal.getId());
        address.setUser(user);
        return addressRepository.save(address);
    }

    @PutMapping("/{id}")
    public Address updateAddress(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id,
            @RequestBody Address addressDetails) {
        System.out.println("Updating address " + id + " with details: " + addressDetails);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!address.getUser().getId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (addressDetails.getRecipientName() != null) {
            address.setRecipientName(addressDetails.getRecipientName());
        }
        if (addressDetails.getPhone() != null) {
            address.setPhone(addressDetails.getPhone());
        }
        if (addressDetails.getProvince() != null) {
            address.setProvince(addressDetails.getProvince());
        }
        if (addressDetails.getCity() != null) {
            address.setCity(addressDetails.getCity());
        }
        if (addressDetails.getDistrict() != null) {
            address.setDistrict(addressDetails.getDistrict());
        }
        if (addressDetails.getDetailedAddress() != null) {
            address.setDetailedAddress(addressDetails.getDetailedAddress());
        }
        if (addressDetails.getIsDefault() != null) {
            address.setIsDefault(addressDetails.getIsDefault());
        }
        if (addressDetails.getAreaCode() != null) {
            address.setAreaCode(addressDetails.getAreaCode());
        }
        if (Boolean.TRUE.equals(addressDetails.getIsDefault())) {
            System.out.println("Setting address " + id + " as default");
            addressRepository.findByUserId(principal.getId()).forEach(addr -> {
                if (Boolean.TRUE.equals(addr.getIsDefault()) && !addr.getId().equals(id)) {
                    System.out.println("Unsetting default status for address " + addr.getId());
                    addr.setIsDefault(false);
                    addressRepository.save(addr);
                }
            });
        }
        return addressRepository.save(address);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAddress(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!address.getUser().getId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        addressRepository.delete(address);
        return ResponseEntity.ok().build();
    }
}