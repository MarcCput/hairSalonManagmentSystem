package za.ac.cput.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.Promotion;
import za.ac.cput.domain.enums.DiscountType;
import za.ac.cput.exception.InvalidOperationException;
import za.ac.cput.exception.ResourceNotFoundException;
import za.ac.cput.factory.PromotionFactory;
import za.ac.cput.repository.PromotionRepository;
import za.ac.cput.service.impl.PromotionServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromotionServiceImpl Tests")
class PromotionServiceImplTest {

    @Mock
    private PromotionRepository repository;

    @InjectMocks
    private PromotionServiceImpl service;

    @Test
    @DisplayName("register() builds and saves a valid promotion")
    void register_savesValidPromotion() {
        when(repository.save(any(Promotion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Promotion result = service.register("summer20", "Summer special", DiscountType.PERCENTAGE,
                new BigDecimal("20"), LocalDate.now(), LocalDate.now().plusDays(30), 100);

        assertNotNull(result);
        assertEquals("SUMMER20", result.getCode());
    }

    @Test
    @DisplayName("register() throws InvalidOperationException when the end date is before the start date")
    void register_throwsOnInvalidDateRange() {
        assertThrows(InvalidOperationException.class, () -> service.register(
                "BADRANGE", "desc", DiscountType.FIXED_AMOUNT, new BigDecimal("50"),
                LocalDate.now().plusDays(5), LocalDate.now(), 10));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("validate() returns the promotion when it's active and under its usage limit")
    void validate_returnsPromotion_whenActive() {
        Promotion active = PromotionFactory.buildPromotion("ACTIVE10", "desc", DiscountType.PERCENTAGE,
                new BigDecimal("10"), LocalDate.now().minusDays(1), LocalDate.now().plusDays(10), 100);
        when(repository.findByCode("ACTIVE10")).thenReturn(Optional.of(active));

        assertEquals(active, service.validate("ACTIVE10"));
    }

    @Test
    @DisplayName("validate() throws ResourceNotFoundException for an unknown code")
    void validate_throws_whenCodeUnknown() {
        when(repository.findByCode("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.validate("NOPE"));
    }

    @Test
    @DisplayName("validate() throws InvalidOperationException for a promotion that hasn't started yet")
    void validate_throws_whenNotYetActive() {
        Promotion future = PromotionFactory.buildPromotion("FUTURE", "desc", DiscountType.PERCENTAGE,
                new BigDecimal("10"), LocalDate.now().plusDays(5), LocalDate.now().plusDays(15), 100);
        when(repository.findByCode("FUTURE")).thenReturn(Optional.of(future));

        assertThrows(InvalidOperationException.class, () -> service.validate("FUTURE"));
    }

    @Test
    @DisplayName("validate() throws InvalidOperationException for an expired promotion")
    void validate_throws_whenExpired() {
        Promotion expired = PromotionFactory.buildPromotion("EXPIRED", "desc", DiscountType.PERCENTAGE,
                new BigDecimal("10"), LocalDate.now().minusDays(30), LocalDate.now().minusDays(1), 100);
        when(repository.findByCode("EXPIRED")).thenReturn(Optional.of(expired));

        assertThrows(InvalidOperationException.class, () -> service.validate("EXPIRED"));
    }

    @Test
    @DisplayName("validate() throws InvalidOperationException once the usage limit is reached")
    void validate_throws_whenUsageLimitReached() {
        Promotion maxedOut = buildPromotionWithUsage(2, 2); // usageCount == usageLimit
        when(repository.findByCode("MAXED")).thenReturn(Optional.of(maxedOut));

        assertThrows(InvalidOperationException.class, () -> service.validate("MAXED"));
    }

    @Test
    @DisplayName("recordUsage() saves a copy of the promotion with usageCount incremented by one")
    void recordUsage_incrementsUsageCount() {
        Promotion promo = PromotionFactory.buildPromotion("SAVE10", "desc", DiscountType.PERCENTAGE,
                new BigDecimal("10"), LocalDate.now(), LocalDate.now().plusDays(10), 100);
        when(repository.save(any(Promotion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Promotion> captor = ArgumentCaptor.forClass(Promotion.class);

        service.recordUsage(promo);

        verify(repository).save(captor.capture());
        assertEquals(promo.getUsageCount() + 1, captor.getValue().getUsageCount());
        // everything else about the promotion should carry over unchanged
        assertEquals(promo.getCode(), captor.getValue().getCode());
        assertEquals(promo.getDiscountValue(), captor.getValue().getDiscountValue());
    }

    @Test
    @DisplayName("getAll() returns every promotion")
    void getAll_returnsAllPromotions() {
        Promotion promo = PromotionFactory.buildPromotion("SAVE10", "desc", DiscountType.PERCENTAGE,
                new BigDecimal("10"), LocalDate.now(), LocalDate.now().plusDays(10), 100);
        when(repository.findAll()).thenReturn(List.of(promo));

        assertEquals(1, service.getAll().size());
    }

    /** Builds a promotion via the factory (usageCount 0) then bumps usageCount via the Builder, for the "maxed out" scenario. */
    private Promotion buildPromotionWithUsage(int usageCount, int usageLimit) {
        Promotion base = PromotionFactory.buildPromotion("MAXED", "desc", DiscountType.PERCENTAGE,
                new BigDecimal("10"), LocalDate.now().minusDays(1), LocalDate.now().plusDays(10), usageLimit);
        assertNotNull(base);
        return new Promotion.Builder()
                .setPromotionId(base.getPromotionId())
                .setCode(base.getCode())
                .setDescription(base.getDescription())
                .setDiscountType(base.getDiscountType())
                .setDiscountValue(base.getDiscountValue())
                .setDateRange(base.getDateRange())
                .setUsageLimit(usageLimit)
                .setUsageCount(usageCount)
                .build();
    }
}

