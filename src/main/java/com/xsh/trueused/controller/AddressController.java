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
    public List<Address> getUserAddresses(@AuthenticationPrincipal UserPrincipal principal) {
        return addressRepository.findByUserId(principal.getId());
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
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!address.getUser().getId().equals(principal.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        address.setRecipientName(addressDetails.getRecipientName());
        address.setPhone(addressDetails.getPhone());
        address.setProvince(addressDetails.getProvince());
        address.setCity(addressDetails.getCity());
        address.setDistrict(addressDetails.getDistrict());
        address.setDetailedAddress(addressDetails.getDetailedAddress());
        address.setIsDefault(addressDetails.getIsDefault());
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