package com.example.ordermanagement.repository;

import com.example.ordermanagement.entity.Inventory;
import com.example.ordermanagement.entity.Product;
import com.example.ordermanagement.enums.ProductStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class InventoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private InventoryRepository inventoryRepository;

    private Product product;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setProductCode("PROD-INV-001");
        product.setName("Inventory Test Product");
        product.setCategory("Electronics");
        product.setPrice(new BigDecimal("149.99"));
        product.setStatus(ProductStatus.ACTIVE);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        entityManager.persist(product);

        inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setAvailableQuantity(100);
        inventory.setReservedQuantity(10);
        inventory.setReorderLevel(20);
        inventory.setUpdatedAt(LocalDateTime.now());
        entityManager.persist(inventory);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findByProductId_withExistingProductId_returnsInventory() {
        Optional<Inventory> result = inventoryRepository.findByProductId(product.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getAvailableQuantity()).isEqualTo(100);
        assertThat(result.get().getReservedQuantity()).isEqualTo(10);
    }

    @Test
    void findByProductId_withNonExistingProductId_returnsEmpty() {
        Optional<Inventory> result = inventoryRepository.findByProductId(999L);

        assertThat(result).isEmpty();
    }

    @Test
    void existsByProductId_withExistingProduct_returnsTrue() {
        assertThat(inventoryRepository.existsByProductId(product.getId())).isTrue();
    }

    @Test
    void existsByProductId_withNonExistingProduct_returnsFalse() {
        assertThat(inventoryRepository.existsByProductId(999L)).isFalse();
    }

    @Test
    void findByProductIdForUpdate_returnsInventoryWithVersion() {
        Optional<Inventory> result = inventoryRepository.findByProductIdForUpdate(product.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isNotNull();
    }

    @Test
    void saveInventory_updatesQuantitiesCorrectly() {
        Inventory inv = inventoryRepository.findByProductId(product.getId()).orElseThrow();
        inv.setAvailableQuantity(80);
        inv.setReservedQuantity(20);
        inventoryRepository.save(inv);

        entityManager.flush();
        entityManager.clear();

        Inventory updated = inventoryRepository.findByProductId(product.getId()).orElseThrow();
        assertThat(updated.getAvailableQuantity()).isEqualTo(80);
        assertThat(updated.getReservedQuantity()).isEqualTo(20);
    }
}
