package za.ac.cput.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.Stylist;
import za.ac.cput.exception.InvalidOperationException;
import za.ac.cput.exception.ResourceNotFoundException;
import za.ac.cput.factory.StylistFactory;
import za.ac.cput.repository.StylistRepository;
import za.ac.cput.service.impl.StylistServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StylistServiceImpl Tests")
class StylistServiceImplTest {

    @Mock
    private StylistRepository repository;

    @InjectMocks
    private StylistServiceImpl service;

    private Stylist sampleStylist;

    @BeforeEach
    void setUp() {
        sampleStylist = StylistFactory.buildStylist("Lebo", "Mokoena", "lebo@salon.com", "0711234567", "Colouring");
    }

    @Test
    @DisplayName("create() saves and returns the stylist")
    void create_savesAndReturnsStylist() {
        when(repository.save(sampleStylist)).thenReturn(sampleStylist);

        Stylist result = service.create(sampleStylist);

        assertEquals(sampleStylist, result);
    }

    @Test
    @DisplayName("read() returns the stylist when found")
    void read_returnsStylist_whenFound() {
        when(repository.findById(sampleStylist.getStylistId())).thenReturn(Optional.of(sampleStylist));

        assertEquals(sampleStylist, service.read(sampleStylist.getStylistId()));
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
        when(repository.findById(sampleStylist.getStylistId())).thenReturn(Optional.of(sampleStylist));
        when(repository.save(sampleStylist)).thenReturn(sampleStylist);

        assertEquals(sampleStylist, service.update(sampleStylist));
    }

    @Test
    @DisplayName("delete() delegates to the repository")
    void delete_callsRepository() {
        service.delete(sampleStylist.getStylistId());

        verify(repository).deleteById(sampleStylist.getStylistId());
    }

    @Test
    @DisplayName("getAll() returns every stylist")
    void getAll_returnsAllStylists() {
        when(repository.findAll()).thenReturn(List.of(sampleStylist));

        assertEquals(1, service.getAll().size());
    }

    @Test
    @DisplayName("register() builds and saves a valid stylist")
    void register_savesValidStylist() {
        when(repository.save(any(Stylist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Stylist result = service.register("Lebo", "Mokoena", "lebo@salon.com", "0711234567", "Colouring");

        assertNotNull(result);
        assertTrue(result.isActive());
        verify(repository).save(any(Stylist.class));
    }

    @Test
    @DisplayName("register() throws InvalidOperationException on bad input, and never saves")
    void register_throwsOnInvalidDetails() {
        assertThrows(InvalidOperationException.class,
                () -> service.register("", "Mokoena", "lebo@salon.com", "0711234567", "Colouring"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("findActiveStylists() delegates to the repository's derived query")
    void findActiveStylists_delegatesToRepository() {
        when(repository.findByIsActiveTrue()).thenReturn(List.of(sampleStylist));

        List<Stylist> result = service.findActiveStylists();

        assertEquals(1, result.size());
        verify(repository).findByIsActiveTrue();
    }
}

