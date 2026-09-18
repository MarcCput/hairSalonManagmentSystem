package za.ac.cput.controller;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.Customer;
import za.ac.cput.service.ICustomerService;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerController Tests")
public class CustomerControllerTest {

    @Mock
    private ICustomerService service;

    @InjectMocks
    private CustomerController controller;

    @Test
    @DisplayName("create() without a date of birth calls register()")
    void createWithoutDateOfBirth_callsRegister() {
        Customer customer = mock(Customer.class);
        when(service.register("Jane", "Doe", "jane@gmail.com",
                "0821234567")).thenReturn(customer);

        CustomerController.CustomerRequest request =
                new CustomerController.CustomerRequest("Jane", "Doe", "jane@gmail.com",
                        "0821234567", null);

        Customer result = controller.create(request);

        assertEquals(customer, result);
        verify(service).register("Jane", "Doe", "jane@gmail.com", "0821234567");
        verify(service, org.mockito.Mockito.never()).registerWithDetails(any(), any(), any(), any(), any());
    }


    @Test
    @DisplayName("create() with a date of birth calls registerWithDetails() instead")
    void create_withDateOfBirth_callsRegisterWithDetails() {
        Customer customer = mock(Customer.class);
        LocalDate dob = LocalDate.of(2000, 1, 1);
        when(service.registerWithDetails("Jane", "Doe", "jane@gmail.com",
                "0821234567", dob)).thenReturn(customer);

        CustomerController.CustomerRequest request =
                new CustomerController.CustomerRequest("Jane", "Doe", "jane@gmail.com",
                        "0821234567", dob);

        Customer result = controller.create(request);

        assertEquals(customer, result);
        verify(service).registerWithDetails("Jane", "Doe", "jane@gmail.com",
                "0821234567", dob);
        verify(service, org.mockito.Mockito.never()).registerWithDetails(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("getAll() delegates to service.getAll()")
    void getAll_delegatesToService() {
        List<Customer> customers = List.of(mock(Customer.class), mock(Customer.class));
        when(service.getAll()).thenReturn(customers);

        assertEquals(customers, controller.getAll());
    }

    @Test
    @DisplayName("getAll() delegates to service.findByEmail()")
    void findByEmail_delegatesToService() {
        Customer customer = mock(Customer.class);
        when(service.findByEmail("jane@gmail.com")).thenReturn(customer);

        assertEquals(customer, controller.findByEmail("jane@gmail.com"));
    }

    @Test
    @DisplayName("delete() delegates to service.delete()")
    void delete_delegatesToService() {
        controller.delete("c1");

        verify(service).delete("c1");
    }
}
