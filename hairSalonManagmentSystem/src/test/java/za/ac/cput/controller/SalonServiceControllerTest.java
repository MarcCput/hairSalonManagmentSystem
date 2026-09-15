package za.ac.cput.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.SalonService;
import za.ac.cput.domain.enums.ServiceCategory;
import za.ac.cput.service.ISalonServiceService;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalonServiceController Tests")
class SalonServiceControllerTest {

    @Mock
    private ISalonServiceService service;

    @InjectMocks
    private SalonServiceController controller;

    @Test
    @DisplayName("create() delegates to service.register()")
    void create_delegatesToService() {
        SalonService salonService = mock(SalonService.class);
        BigDecimal price = new BigDecimal("150.00");
        when(service.register("Classic Haircut", "A trim", 30, price,
                ServiceCategory.HAIRCUT)).thenReturn(salonService);

        SalonServiceController.SalonServiceRequest request = new SalonServiceController.
                SalonServiceRequest("Classic Haircut", "A trim", 30,
                price, ServiceCategory.HAIRCUT);

        assertEquals(salonService, controller.create(request));
    }

    @Test
    @DisplayName("update() rebuilds the service, preserving the existing active flag, and saves it")
    void update_preservesActiveFlag_andSaves() {
        SalonService existing = mock(SalonService.class);
        when(existing.isActive()).thenReturn(true);
        when(service.read("svc1")).thenReturn(existing);

        SalonService updated = mock(SalonService.class);
        when(service.update(any(SalonService.class))).thenReturn(updated);

        BigDecimal price = new BigDecimal("175.00");
        SalonServiceController.SalonServiceRequest request = new SalonServiceController.
                SalonServiceRequest("Classic Haircut", "Edited", 35,
                price, ServiceCategory.HAIRCUT);

        SalonService result = controller.update("svc1", request);

        assertEquals(updated, result);

        ArgumentCaptor<SalonService> captor = ArgumentCaptor.forClass(SalonService.class);
        verify(service).update(captor.capture());
        SalonService saved = captor.getValue();
        assertEquals("svc1", saved.getServiceId());
        assertEquals("Classic Haircut", saved.getName());
        assertEquals(35, saved.getDurationMinutes());
        assertEquals(0, price.compareTo(saved.getPrice().getValue()));
        assertTrue(saved.isActive(), "active flag should carry over from the existing record, " +
                "not the request");
    }

    @Test
    @DisplayName("update() reads the existing record before saving")
    void update_readsExistingRecordFirst() {
        SalonService existing = mock(SalonService.class);
        when(existing.isActive()).thenReturn(false);
        when(service.read("svc1")).thenReturn(existing);
        when(service.update(any(SalonService.class))).thenReturn(mock(SalonService.class));

        SalonServiceController.SalonServiceRequest request = new SalonServiceController.
                SalonServiceRequest("Classic Haircut", null, 30,
                new BigDecimal("150.00"), ServiceCategory.HAIRCUT);

        controller.update("svc1", request);

        verify(service).read("svc1");
    }

    @Test
    @DisplayName("read() delegates to service.read()")
    void read_delegatesToService() {
        SalonService salonService = mock(SalonService.class);
        when(service.read("svc1")).thenReturn(salonService);

        assertEquals(salonService, controller.read("svc1"));
    }

    @Test
    @DisplayName("getAll() delegates to service.getAll()")
    void getAll_delegatesToService() {
        List<SalonService> services = List.of(mock(SalonService.class));
        when(service.getAll()).thenReturn(services);

        assertEquals(services, controller.getAll());
    }

    @Test
    @DisplayName("findActive() delegates to service.findActiveServices()")
    void findActive_delegatesToService() {
        List<SalonService> services = List.of(mock(SalonService.class));
        when(service.findActiveServices()).thenReturn(services);

        assertEquals(services, controller.findActive());
    }

    @Test
    @DisplayName("findByCategory() delegates to service.findByCategory()")
    void findByCategory_delegatesToService() {
        List<SalonService> services = List.of(mock(SalonService.class));
        when(service.findByCategory(ServiceCategory.STYLING)).thenReturn(services);

        assertEquals(services, controller.findByCategory(ServiceCategory.STYLING));
    }

    @Test
    @DisplayName("delete() delegates to service.delete()")
    void delete_delegatesToService() {
        controller.delete("svc1");

        verify(service).delete("svc1");
    }
}

