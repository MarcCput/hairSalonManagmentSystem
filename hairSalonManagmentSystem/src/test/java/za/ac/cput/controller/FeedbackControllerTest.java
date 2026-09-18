package za.ac.cput.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.Feedback;
import za.ac.cput.service.IFeedbackService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackController Tests")
public class FeedbackControllerTest {

    @Mock
    private IFeedbackService service;

    @InjectMocks
    private FeedbackController controller;

    @Test
    @DisplayName("create() delegates to service.submitFeedback()")
    void create_delegatesToService(){
        Feedback feedback = mock(Feedback.class);
        when(service.submitFeedback("a1", "c1", 5, "Loved it"))
                .thenReturn(feedback);

        FeedbackController.FeedbackRequest request =
                new FeedbackController.FeedbackRequest("a1", "c1", 5,
                        "Loved it");

        assertEquals(feedback, controller.create(request));
    }

    @Test
    @DisplayName("read() delegates to service.read()")
    void read_delegatesToService(){
        Feedback feedback = mock(Feedback.class);
        when(service.read("a1")).thenReturn(feedback);

        assertEquals(feedback, controller.read("a1"));
    }

    @Test
    @DisplayName("getAll() delegates to service.getAll()")
    void getAll_delegatesToService(){
        List<Feedback> feedbacks = List.of(mock(Feedback.class));
        when(service.getAll()).thenReturn(feedbacks);

        assertEquals(feedbacks, controller.getAll());
    }

    @Test
    @DisplayName("delete() delegates to service.delete()")
    void delete_delegatesToService(){
        controller.delete("f1");

        verify(service).delete("f1");
    }
}
