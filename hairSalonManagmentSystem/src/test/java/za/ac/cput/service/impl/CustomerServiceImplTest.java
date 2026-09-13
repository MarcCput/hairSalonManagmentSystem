package za.ac.cput.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.Customer;
import za.ac.cput.exception.InvalidOperationException;
import za.ac.cput.exception.ResourceNotFoundException;
import za.ac.cput.factory.CustomerFactory;
import za.ac.cput.repository.CustomerRepository;
import za.ac.cput.service.impl.CustomerServiceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Mockito unit tests for CustomerServiceImpl.
 * The repository is mocked, so these tests exercise the service's own logic
 * (validation via the factory, exception translation) without touching a real database
 */

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerServiceImpl Tests")
public class CustomerServiceImplTest {

    @Mock
    private CustomerRepository repository;

    @InjectMocks
    private CustomerServiceImpl service;

    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        sampleCustomer = CustomerFactory.buildCustomer("Jane", "Doe", "jane@example.com", "0821234567");
    }

    @Test
    @DisplayName("create() saves and returns the customer")
    void create_savesAndReturnsCustomer() {
        when(repository.save(sampleCustomer)).thenReturn(sampleCustomer);

        Customer result = service.create(sampleCustomer);

        assertEquals(sampleCustomer, result);
        verify(repository).save(sampleCustomer);
    }

    @Test
    @DisplayName("read() returns the customer when found")
    void read_returnsCustomer_whenFound() {
        when(repository.findById(sampleCustomer.getCustomerId())).thenReturn(Optional.of(sampleCustomer));

        Customer result = service.read(sampleCustomer.getCustomerId());

        assertEquals(sampleCustomer, result);
    }

    @Test
    @DisplayName("read() throws ResourceNotFoundException when the id doesn't exist")
    void read_throws_whenNotFound() {
        when(repository.findById("missing-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.read("missing-id"));
    }

    @Test
    @DisplayName("update() checks the customer exists, then saves")
    void update_savesWhenExists() {
        when(repository.findById(sampleCustomer.getCustomerId())).thenReturn(Optional.of(sampleCustomer));
        when(repository.save(sampleCustomer)).thenReturn(sampleCustomer);

        Customer result = service.update(sampleCustomer);

        assertEquals(sampleCustomer, result);
        verify(repository).save(sampleCustomer);
    }

    @Test
    @DisplayName("update() throws when the customer being updated doesn't exist")
    void update_throws_whenNotFound() {
        when(repository.findById(sampleCustomer.getCustomerId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(sampleCustomer));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("delete() delegates straight to the repository")
    void delete_callsRepository() {
        service.delete(sampleCustomer.getCustomerId());

        verify(repository).deleteById(sampleCustomer.getCustomerId());
    }

    @Test
    @DisplayName("getAll() returns everything the repository has")
    void getAll_returnsAllCustomers() {
        when(repository.findAll()).thenReturn(List.of(sampleCustomer));

        List<Customer> result = service.getAll();

        assertEquals(1, result.size());
        assertEquals(sampleCustomer, result.get(0));
    }

    @Test
    @DisplayName("register() builds a customer via the factory and saves it")
    void register_savesValidCustomer() {
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer result = service.register("Jane", "Doe", "jane@example.com", "0821234567");

        assertNotNull(result);
        assertEquals("Jane", result.getFirstName());
        assertEquals("jane@example.com", result.getEmail().getValue());
        verify(repository).save(any(Customer.class));
    }

    @Test
    @DisplayName("register() throws InvalidOperationException for a bad email, and never saves")
    void register_throwsOnInvalidDetails() {
        assertThrows(InvalidOperationException.class,
                () -> service.register("Jane", "Doe", "not-an-email", "0821234567"));

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("registerWithDetails() carries the date of birth through to the saved customer")
    void registerWithDetails_savesDateOfBirth() {
        LocalDate dob = LocalDate.of(1995, 6, 15);
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer result = service.registerWithDetails("Jane", "Doe", "jane@example.com", "0821234567", dob);

        assertEquals(dob, result.getDateOfBirth());
    }

    @Test
    @DisplayName("findByEmail() returns the customer when the email is registered")
    void findByEmail_returnsCustomer_whenFound() {
        when(repository.findByEmail_Value("jane@example.com")).thenReturn(Optional.of(sampleCustomer));

        Customer result = service.findByEmail("jane@example.com");

        assertEquals(sampleCustomer, result);
    }

    @Test
    @DisplayName("findByEmail() throws ResourceNotFoundException when no customer has that email")
    void findByEmail_throws_whenNotFound() {
        when(repository.findByEmail_Value("nobody@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findByEmail("nobody@example.com"));
    }


}
