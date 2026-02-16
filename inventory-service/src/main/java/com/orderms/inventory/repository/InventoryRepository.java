package com.orderms.inventory.repository;

import com.orderms.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Lock(LockModeType.OPTIMISTIC)
    Optional<Inventory> findByProductId(String productId);

    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId AND i.availableStock >= :quantity")
    Optional<Inventory> findByProductIdWithSufficientStock(
            @Param("productId") String productId,
            @Param("quantity") Integer quantity
    );
}