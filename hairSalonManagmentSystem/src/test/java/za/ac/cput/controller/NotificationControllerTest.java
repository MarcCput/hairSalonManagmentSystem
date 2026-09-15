package za.ac.cput.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.Notification;
import za.ac.cput.domain.enums.NotificationChannel;
import za.ac.cput.service.INotificationService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController Tests")
class NotificationControllerTest {

    @Mock
    private INotificationService service;

    @InjectMocks
    private NotificationController controller;

    @Test
    @DisplayName("scheduleReminder() delegates to service.scheduleAppointmentReminder()")
    void scheduleReminder_delegatesToService() {
        Notification notification = mock(Notification.class);
        when(service.scheduleAppointmentReminder("a1", NotificationChannel.EMAIL)).thenReturn(notification);

        NotificationController.AppointmentReminderRequest request =
                new NotificationController.AppointmentReminderRequest("a1", NotificationChannel.EMAIL);

        assertEquals(notification, controller.scheduleReminder(request));
    }

    @Test
    @DisplayName("sendPromotion() delegates to service.sendPromotionNotification()")
    void sendPromotion_delegatesToService() {
        Notification notification = mock(Notification.class);
        when(service.sendPromotionNotification("20% off!", NotificationChannel.SMS)).thenReturn(notification);

        NotificationController.PromotionNotificationRequest request =
                new NotificationController.PromotionNotificationRequest("20% off!", NotificationChannel.SMS);

        assertEquals(notification, controller.sendPromotion(request));
    }

    @Test
    @DisplayName("read() delegates to service.read()")
    void read_delegatesToService() {
        Notification notification = mock(Notification.class);
        when(service.read("n1")).thenReturn(notification);

        assertEquals(notification, controller.read("n1"));
    }

    @Test
    @DisplayName("findByCustomer() delegates to service.findByCustomer()")
    void findByCustomer_delegatesToService() {
        List<Notification> notifications = List.of(mock(Notification.class));
        when(service.findByCustomer("c1")).thenReturn(notifications);

        assertEquals(notifications, controller.findByCustomer("c1"));
    }

    @Test
    @DisplayName("getAll() delegates to service.getAll()")
    void getAll_delegatesToService() {
        List<Notification> notifications = List.of(mock(Notification.class));
        when(service.getAll()).thenReturn(notifications);

        assertEquals(notifications, controller.getAll());
    }

    @Test
    @DisplayName("delete() delegates to service.delete()")
    void delete_delegatesToService() {
        controller.delete("n1");

        verify(service).delete("n1");
    }
}

