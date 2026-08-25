package com.example.demo.service;

import com.example.demo.entity.Address;
import com.example.demo.entity.User;
import com.example.demo.repository.AddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    public List<Address> getAddressesForUser(User user) {
        return addressRepository.findByUserId(user.getUserId());
    }

    @Transactional
    public Address addAddress(User user, Address address) {
        address.setUser(user);
        address.setCreatedAt(LocalDateTime.now());
        address.setUpdatedAt(LocalDateTime.now());

        if (address.isDefault()) {
            // Unset other default addresses
            List<Address> addresses = addressRepository.findByUserId(user.getUserId());
            for (Address a : addresses) {
                if (a.isDefault()) {
                    a.setDefault(false);
                    addressRepository.save(a);
                }
            }
        } else {
            // If it is the first address, make it default automatically
            List<Address> addresses = addressRepository.findByUserId(user.getUserId());
            if (addresses.isEmpty()) {
                address.setDefault(true);
            }
        }

        return addressRepository.save(address);
    }

    @Transactional
    public Address updateAddress(User user, int addressId, Address addressDetails) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!address.getUser().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Unauthorized to update this address");
        }

        address.setFullName(addressDetails.getFullName());
        address.setPhoneNumber(addressDetails.getPhoneNumber());
        address.setStreetAddress(addressDetails.getStreetAddress());
        address.setCity(addressDetails.getCity());
        address.setState(addressDetails.getState());
        address.setZipCode(addressDetails.getZipCode());
        address.setUpdatedAt(LocalDateTime.now());

        if (addressDetails.isDefault() && !address.isDefault()) {
            // Unset other default addresses
            List<Address> addresses = addressRepository.findByUserId(user.getUserId());
            for (Address a : addresses) {
                if (a.isDefault() && !a.getId().equals(address.getId())) {
                    a.setDefault(false);
                    addressRepository.save(a);
                }
            }
            address.setDefault(true);
        }

        return addressRepository.save(address);
    }

    @Transactional
    public void deleteAddress(User user, int addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!address.getUser().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Unauthorized to delete this address");
        }

        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);

        // If the deleted address was default, make the next one default
        if (wasDefault) {
            List<Address> addresses = addressRepository.findByUserId(user.getUserId());
            if (!addresses.isEmpty()) {
                Address nextDefault = addresses.get(0);
                nextDefault.setDefault(true);
                addressRepository.save(nextDefault);
            }
        }
    }

    @Transactional
    public Address setDefaultAddress(User user, int addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (!address.getUser().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Unauthorized to modify this address");
        }

        List<Address> addresses = addressRepository.findByUserId(user.getUserId());
        for (Address a : addresses) {
            if (a.isDefault()) {
                a.setDefault(false);
                addressRepository.save(a);
            }
        }

        address.setDefault(true);
        address.setUpdatedAt(LocalDateTime.now());
        return addressRepository.save(address);
    }
}
