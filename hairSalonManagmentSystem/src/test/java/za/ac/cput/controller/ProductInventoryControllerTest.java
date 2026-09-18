package za.ac.cput.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.ProductInventory;
import za.ac.cput.domain.SalonService;
import za.ac.cput.service.IProductInventoryService;
import za.ac.cput.service.ISalonServiceService;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductInventoryController Tests")
class ProductInventoryControllerTest {

    @Mock
    private IProductInventoryService service;

    @Mock
    private ISalonServiceService salonServiceService;

    @InjectMocks
    private ProductInventoryController controller;

    @Test
    @DisplayName("create() with a salonServiceId looks up that service first")
    void create_withSalonServiceId_looksUpService() {
        SalonService salonService = mock(SalonService.class);
        ProductInventory product = mock(ProductInventory.class);
        when(salonServiceService.read("svc1")).thenReturn(salonService);
        when(service.register("Shampoo", "BrandX", "Hair Care", 10, 3,
                new BigDecimal("20.00"), new BigDecimal("40.00"), salonService)).thenReturn(product);

        ProductInventoryController.ProductRequest request = new ProductInventoryController.ProductRequest(
                "Shampoo", "BrandX", "Hair Care", 10, 3,
                new BigDecimal("20.00"), new BigDecimal("40.00"), "svc1");

        assertEquals(product, controller.create(request));
        verify(salonServiceService).read("svc1");
    }

    @Test
    @DisplayName("create() without a salonServiceId never touches salonServiceService")
    void create_withoutSalonServiceId_skipsLookup() {
        ProductInventory product = mock(ProductInventory.class);
        when(service.register("Shampoo", "BrandX", "Hair Care", 10, 3,
                new BigDecimal("20.00"), new BigDecimal("40.00"), null)).thenReturn(product);

        ProductInventoryController.ProductRequest request = new ProductInventoryController.ProductRequest(
                "Shampoo", "BrandX", "Hair Care", 10, 3,
                new BigDecimal("20.00"), new BigDecimal("40.00"), null);

        assertEquals(product, controller.create(request));
        verifyNoInteractions(salonServiceService);
    }

    @Test
    @DisplayName("update() preserves the existing stock quantity, ignoring the request's value")
    void update_preservesExistingStockQuantity() {
        ProductInventory existing = mock(ProductInventory.class);
        when(existing.getStockQuantity()).thenReturn(42);
        when(service.read("p1")).thenReturn(existing);
        when(service.update(any(ProductInventory.class))).thenReturn(mock(ProductInventory.class));

        // Request carries a different stockQuantity (999) - the controller must ignore it and
        // keep the existing value, since /adjust-stock is the only sanctioned way to change it.
        ProductInventoryController.ProductRequest request = new ProductInventoryController.ProductRequest(
                "Shampoo", "BrandX", "Hair Care", 999, 5,
                new BigDecimal("22.00"), new BigDecimal("45.00"), null);

        controller.update("p1", request);

        ArgumentCaptor<ProductInventory> captor = ArgumentCaptor.forClass(ProductInventory.class);
        verify(service).update(captor.capture());
        assertEquals(42, captor.getValue().getStockQuantity());
    }

    @Test
    @DisplayName("read() delegates to service.read()")
    void read_delegatesToService() {
        ProductInventory product = mock(ProductInventory.class);
        when(service.read("p1")).thenReturn(product);

        assertEquals(product, controller.read("p1"));
    }

    @Test
    @DisplayName("getAll() delegates to service.getAll()")
    void getAll_delegatesToService() {
        List<ProductInventory> products = List.of(mock(ProductInventory.class));
        when(service.getAll()).thenReturn(products);

        assertEquals(products, controller.getAll());
    }

    @Test
    @DisplayName("lowStock() delegates to service.lowStock()")
    void lowStock_delegatesToService() {
        List<ProductInventory> products = List.of(mock(ProductInventory.class));
        when(service.lowStock()).thenReturn(products);

        assertEquals(products, controller.lowStock());
    }

    @Test
    @DisplayName("adjustStock() delegates to service.adjustStock()")
    void adjustStock_delegatesToService() {
        ProductInventory product = mock(ProductInventory.class);
        when(service.adjustStock("p1", 10)).thenReturn(product);

        ProductInventoryController.AdjustStockRequest request = new ProductInventoryController.AdjustStockRequest(10);

        assertEquals(product, controller.adjustStock("p1", request));
    }

    @Test
    @DisplayName("delete() delegates to service.delete()")
    void delete_delegatesToService() {
        controller.delete("p1");

        verify(service).delete("p1");
    }
}

