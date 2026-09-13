package za.ac.cput.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.SalonService;
import za.ac.cput.domain.enums.ServiceCategory;
import za.ac.cput.exception.InvalidOperationException;
import za.ac.cput.exception.ResourceNotFoundException;
import za.ac.cput.factory.SalonServiceFactory;
import za.ac.cput.repository.SalonServiceRepository;
import za.ac.cput.service.impl.SalonServiceServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalonServiceServiceImpl Tests")
class SalonServiceServiceImplTest {

    @Mock
    private SalonServiceRepository repository;

    @InjectMocks
    private SalonServiceServiceImpl service;

    private SalonService sampleService;

    @BeforeEach
    void setUp() {
        sampleService = SalonServiceFactory.buildService(
                "Blowout", "Full blowout and style", 60, new BigDecimal("200.00"), ServiceCategory.STYLING);
    }

    @Test
    @DisplayName("create() saves and returns the service")
    void create_savesAndReturnsService() {
        when(repository.save(sampleService)).thenReturn(sampleService);

        assertEquals(sampleService, service.create(sampleService));
    }

    @Test
    @DisplayName("read() returns the service when found")
    void read_returnsService_whenFound() {
        when(repository.findById(sampleService.getServiceId())).thenReturn(Optional.of(sampleService));

        assertEquals(sampleService, service.read(sampleService.getServiceId()));
    }

    @Test
    @DisplayName("read() throws ResourceNotFoundException when missing")
    void read_throws_whenNotFound() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.read("missing"));
    }

    @Test
    @DisplayName("update() checks existence, then saves")
    void update_savesWhenExists() {
        when(repository.findById(sampleService.getServiceId())).thenReturn(Optional.of(sampleService));
        when(repository.save(sampleService)).thenReturn(sampleService);

        assertEquals(sampleService, service.update(sampleService));
    }

    @Test
    @DisplayName("delete() delegates to the repository")
    void delete_callsRepository() {
        service.delete(sampleService.getServiceId());

        verify(repository).deleteById(sampleService.getServiceId());
    }

    @Test
    @DisplayName("getAll() returns every service")
    void getAll_returnsAllServices() {
        when(repository.findAll()).thenReturn(List.of(sampleService));

        assertEquals(1, service.getAll().size());
    }

    @Test
    @DisplayName("register() builds and saves a valid service")
    void register_savesValidService() {
        when(repository.save(any(SalonService.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SalonService result = service.register(
                "Blowout", "Full blowout and style", 60, new BigDecimal("200.00"), ServiceCategory.STYLING);

        assertNotNull(result);
        assertEquals("Blowout", result.getName());
    }

    @Test
    @DisplayName("register() throws InvalidOperationException for a non-positive price")
    void register_throwsOnInvalidDetails() {
        assertThrows(InvalidOperationException.class,
                () -> service.register("Blowout", "desc", 60, BigDecimal.ZERO, ServiceCategory.STYLING));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("findByCategory() delegates to the repository's derived query")
    void findByCategory_delegatesToRepository() {
        when(repository.findByCategory(ServiceCategory.STYLING)).thenReturn(List.of(sampleService));

        List<SalonService> result = service.findByCategory(ServiceCategory.STYLING);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("findActiveServices() delegates to the repository's derived query")
    void findActiveServices_delegatesToRepository() {
        when(repository.findByIsActiveTrue()).thenReturn(List.of(sampleService));

        assertEquals(1, service.findActiveServices().size());
    }
}

