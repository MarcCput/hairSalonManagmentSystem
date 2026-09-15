package za.ac.cput.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.Stylist;
import za.ac.cput.service.IStylistService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StylistController Tests")
class StylistControllerTest {

    @Mock
    private IStylistService service;

    @InjectMocks
    private StylistController controller;

    @Test
    @DisplayName("create() delegates to service.register()")
    void create_delegatesToService() {
        Stylist stylist = mock(Stylist.class);
        when(service.register("Lebo", "Mokoena", "lebo@salon.com",
                "0711234567", "Colouring")).thenReturn(stylist);

        StylistController.StylistRequest request =
                new StylistController.StylistRequest("Lebo", "Mokoena",
                        "lebo@salon.com", "0711234567", "Colouring");

        assertEquals(stylist, controller.create(request));
    }

    @Test
    @DisplayName("read() delegates to service.read()")
    void read_delegatesToService() {
        Stylist stylist = mock(Stylist.class);
        when(service.read("s1")).thenReturn(stylist);

        assertEquals(stylist, controller.read("s1"));
    }

    @Test
    @DisplayName("getAll() delegates to service.getAll()")
    void getAll_delegatesToService() {
        List<Stylist> stylists = List.of(mock(Stylist.class));
        when(service.getAll()).thenReturn(stylists);

        assertEquals(stylists, controller.getAll());
    }

    @Test
    @DisplayName("findActive() delegates to service.findActiveStylists()")
    void findActive_delegatesToService() {
        List<Stylist> stylists = List.of(mock(Stylist.class));
        when(service.findActiveStylists()).thenReturn(stylists);

        assertEquals(stylists, controller.findActive());
    }

    @Test
    @DisplayName("delete() delegates to service.delete()")
    void delete_delegatesToService() {
        controller.delete("s1");

        verify(service).delete("s1");
    }
}

