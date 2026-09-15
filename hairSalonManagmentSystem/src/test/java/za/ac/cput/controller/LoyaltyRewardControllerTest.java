package za.ac.cput.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.domain.LoyaltyReward;
import za.ac.cput.service.ILoyaltyRewardService;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoyaltyRewardController Tests")
public class LoyaltyRewardControllerTest {

    @Mock
    private ILoyaltyRewardService service;

    @InjectMocks
    private LoyaltyRewardController controller;

    @Test
    @DisplayName("getOrCreate() delegates to service.getOrCreate()")
    void getOrCreate_delegatesToService(){
        LoyaltyReward reward = mock(LoyaltyReward.class);
        when(service.getOrCreate("c1")).thenReturn(reward);

        assertEquals(reward, controller.getOrCreate("c1"));
    }

    @Test
    @DisplayName("addPoints() delegates to service.addPoints()")
    void addPoints_delegatesToService(){
        LoyaltyReward reward = mock(LoyaltyReward.class);
        BigDecimal amount = new BigDecimal("150.00");
        when(service.addPoints("c1", amount)).thenReturn(reward);

        LoyaltyRewardController.AddPointsRequest request =
                new LoyaltyRewardController.AddPointsRequest(amount);

        assertEquals(reward, controller.addPoints("c1", request));
    }

    @Test
    @DisplayName("redeemPoints() delegates to service.redeemPoints()")
    void redeemPoints_delegatesToService() {
        LoyaltyReward reward = mock(LoyaltyReward.class);
        when(service.redeemPoints("c1", 50)).thenReturn(reward);

        LoyaltyRewardController.RedeemPointsRequest request = new LoyaltyRewardController.RedeemPointsRequest(50);

        assertEquals(reward, controller.redeemPoints("c1", request));
    }

    @Test
    @DisplayName("read() delegates to service.read()")
    void read_delegatesToService() {
        LoyaltyReward reward = mock(LoyaltyReward.class);
        when(service.read("l1")).thenReturn(reward);

        assertEquals(reward, controller.read("l1"));
    }

    @Test
    @DisplayName("getAll() delegates to service.getAll()")
    void getAll_delegatesToService() {
        List<LoyaltyReward> rewards = List.of(mock(LoyaltyReward.class));
        when(service.getAll()).thenReturn(rewards);

        assertEquals(rewards, controller.getAll());
    }

    @Test
    @DisplayName("delete() delegates to service.delete()")
    void delete_delegatesToService() {
        controller.delete("l1");

        verify(service).delete("l1");
    }

}
