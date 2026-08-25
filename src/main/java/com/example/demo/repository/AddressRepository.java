package com.example.demo.repository;

import com.example.demo.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Integer> {
    @Query("SELECT a FROM Address a WHERE a.user.userId = :userId ORDER BY a.isDefault DESC, a.createdAt DESC")
    List<Address> findByUserId(int userId);

    @Query("SELECT a FROM Address a WHERE a.user.userId = :userId AND a.isDefault = true")
    Optional<Address> findDefaultByUserId(int userId);
}
