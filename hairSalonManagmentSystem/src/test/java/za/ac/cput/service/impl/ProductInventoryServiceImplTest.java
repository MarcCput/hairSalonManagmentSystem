package za.ac.cput.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.ProductInventory;
import za.ac.cput.exception.InvalidOperationException;
import za.ac.cput.exception.ResourceNotFoundException;
import za.ac.cput.factory.ProductInventoryFactory;
import za.ac.cput.repository.ProductInventoryRepository;
import za.ac.cput.service.impl.ProductInventoryServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductInventoryServiceImpl Tests")
class ProductInventoryServiceImplTest {

    @Mock
    private ProductInventoryRepository repository;

    @InjectMocks
    private ProductInventoryServiceImpl service;

    private ProductInventory sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = ProductInventoryFactory.buildProduct(
                "Argan Oil Shampoo", "OGX", "Hair Care", 50, 10,
                new BigDecimal("80.00"), new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("register() builds and saves a valid product")
    void register_savesValidProduct() {
        when(repository.save(any(ProductInventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductInventory result = service.register("Argan Oil Shampoo", "OGX", "Hair Care", 50, 10,
                new BigDecimal("80.00"), new BigDecimal("150.00"), null);

        assertNotNull(result);
        assertEquals("Argan Oil Shampoo", result.getName());
    }

    @Test
    @DisplayName("register() throws InvalidOperationException for a zero selling price")
    void register_throwsOnInvalidDetails() {
        assertThrows(InvalidOperationException.class, () -> service.register(
                "Shampoo", "OGX", "Hair Care", 20, 5, new BigDecimal("80"), BigDecimal.ZERO, null));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("adjustStock() increases the stock quantity by a positive delta")
    void adjustStock_increasesQuantity() {
        when(repository.findById(sampleProduct.getProductId())).thenReturn(Optional.of(sampleProduct));
        when(repository.save(any(ProductInventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<ProductInventory> captor = ArgumentCaptor.forClass(ProductInventory.class);

        service.adjustStock(sampleProduct.getProductId(), 20);

        verify(repository).save(captor.capture());
        assertEquals(70, captor.getValue().getStockQuantity()); // 50 + 20
    }

    @Test
    @DisplayName("adjustStock() decreases the stock quantity by a negative delta")
    void adjustStock_decreasesQuantity() {
        when(repository.findById(sampleProduct.getProductId())).thenReturn(Optional.of(sampleProduct));
        when(repository.save(any(ProductInventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductInventory result = service.adjustStock(sampleProduct.getProductId(), -30);

        assertEquals(20, result.getStockQuantity()); // 50 - 30
    }

    @Test
    @DisplayName("adjustStock() throws InvalidOperationException if the adjustment would go negative")
    void adjustStock_throws_whenResultWouldBeNegative() {
        when(repository.findById(sampleProduct.getProductId())).thenReturn(Optional.of(sampleProduct));

        assertThrows(InvalidOperationException.class,
                () -> service.adjustStock(sampleProduct.getProductId(), -100)); // 50 - 100 < 0

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("adjustStock() throws ResourceNotFoundException when the product doesn't exist")
    void adjustStock_throws_whenProductMissing() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.adjustStock("missing", 10));
    }

    @Test
    @DisplayName("lowStock() returns only products at or below their reorder level")
    void lowStock_filtersCorrectly() {
        ProductInventory lowStockProduct = ProductInventoryFactory.buildProduct(
                "Conditioner", "OGX", "Hair Care", 5, 10, // stock (5) <= reorderLevel (10)
                new BigDecimal("80.00"), new BigDecimal("150.00"));
        ProductInventory wellStockedProduct = sampleProduct; // stock (50) > reorderLevel (10)
        when(repository.findAll()).thenReturn(List.of(lowStockProduct, wellStockedProduct));

        List<ProductInventory> result = service.lowStock();

        assertEquals(1, result.size());
        assertEquals("Conditioner", result.get(0).getName());
    }

    @Test
    @DisplayName("getAll() returns every product")
    void getAll_returnsAllProducts() {
        when(repository.findAll()).thenReturn(List.of(sampleProduct));

        assertEquals(1, service.getAll().size());
    }
}

