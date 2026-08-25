package com.example.demo.controller;

import com.example.demo.entity.Address;
import com.example.demo.entity.User;
import com.example.demo.service.AddressService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5174", allowCredentials = "true")
@RequestMapping("/api/addresses")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @GetMapping
    public ResponseEntity<?> getAddresses(HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        List<Address> addresses = addressService.getAddressesForUser(user);
        return ResponseEntity.ok(addresses);
    }

    @PostMapping
    public ResponseEntity<?> addAddress(@RequestBody Address address, HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            Address saved = addressService.addAddress(user, address);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAddress(@PathVariable("id") int addressId, @RequestBody Address address, HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            Address updated = addressService.updateAddress(user, addressId, address);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAddress(@PathVariable("id") int addressId, HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            addressService.deleteAddress(user, addressId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<?> setDefaultAddress(@PathVariable("id") int addressId, HttpServletRequest request) {
        User user = (User) request.getAttribute("authenticatedUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            Address updated = addressService.setDefaultAddress(user, addressId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
