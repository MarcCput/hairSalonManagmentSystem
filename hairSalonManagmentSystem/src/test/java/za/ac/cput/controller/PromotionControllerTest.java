package za.ac.cput.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.Promotion;
import za.ac.cput.domain.enums.DiscountType;
import za.ac.cput.service.IPromotionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromotionController Tests")
class PromotionControllerTest {

    @Mock
    private IPromotionService service;

    @InjectMocks
    private PromotionController controller;

    @Test
    @DisplayName("create() delegates to service.register()")
    void create_delegatesToService() {
        Promotion promotion = mock(Promotion.class);
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusMonths(1);
        when(service.register("SUMMER20", "Summer discount", DiscountType.PERCENTAGE,
                new BigDecimal("20"), start, end, 100)).thenReturn(promotion);

        PromotionController.PromotionRequest request = new PromotionController.PromotionRequest(
                "SUMMER20", "Summer discount", DiscountType.PERCENTAGE, new BigDecimal("20"), start, end, 100);

        assertEquals(promotion, controller.create(request));
    }

    @Test
    @DisplayName("read() delegates to service.read()")
    void read_delegatesToService() {
        Promotion promotion = mock(Promotion.class);
        when(service.read("p1")).thenReturn(promotion);

        assertEquals(promotion, controller.read("p1"));
    }

    @Test
    @DisplayName("getAll() delegates to service.getAll()")
    void getAll_delegatesToService() {
        List<Promotion> promotions = List.of(mock(Promotion.class));
        when(service.getAll()).thenReturn(promotions);

        assertEquals(promotions, controller.getAll());
    }

    @Test
    @DisplayName("validate() delegates to service.validate()")
    void validate_delegatesToService() {
        Promotion promotion = mock(Promotion.class);
        when(service.validate("SUMMER20")).thenReturn(promotion);

        assertEquals(promotion, controller.validate("SUMMER20"));
    }

    @Test
    @DisplayName("delete() delegates to service.delete()")
    void delete_delegatesToService() {
        controller.delete("p1");

        verify(service).delete("p1");
    }
}

