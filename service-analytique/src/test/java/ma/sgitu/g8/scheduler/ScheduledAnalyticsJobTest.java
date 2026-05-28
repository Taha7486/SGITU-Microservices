package ma.sgitu.g8.scheduler;

import ma.sgitu.g8.aggregation.IncidentAggregation;
import ma.sgitu.g8.aggregation.RevenueAggregation;
import ma.sgitu.g8.aggregation.SubscriptionAggregation;
import ma.sgitu.g8.aggregation.TicketAggregation;
import ma.sgitu.g8.aggregation.UserAggregation;
import ma.sgitu.g8.aggregation.VehicleAggregation;
import ma.sgitu.g8.alert.ThresholdAlertService;
import ma.sgitu.g8.ml.MlPredictionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduledAnalyticsJobTest {

    @Mock private IncidentAggregation incidentAggregation;
    @Mock private VehicleAggregation vehicleAggregation;
    @Mock private TicketAggregation ticketAggregation;
    @Mock private RevenueAggregation revenueAggregation;
    @Mock private SubscriptionAggregation subscriptionAggregation;
    @Mock private UserAggregation userAggregation;
    @Mock private ThresholdAlertService thresholdAlertService;
    @Mock private MlPredictionService mlPredictionService;

    private ScheduledAnalyticsJob scheduledAnalyticsJob;

    @BeforeEach
    void setUp() {
        scheduledAnalyticsJob = new ScheduledAnalyticsJob();
        ReflectionTestUtils.setField(scheduledAnalyticsJob, "incidentAggregation", incidentAggregation);
        ReflectionTestUtils.setField(scheduledAnalyticsJob, "vehicleAggregation", vehicleAggregation);
        ReflectionTestUtils.setField(scheduledAnalyticsJob, "ticketAggregation", ticketAggregation);
        ReflectionTestUtils.setField(scheduledAnalyticsJob, "revenueAggregation", revenueAggregation);
        ReflectionTestUtils.setField(scheduledAnalyticsJob, "subscriptionAggregation", subscriptionAggregation);
        ReflectionTestUtils.setField(scheduledAnalyticsJob, "userAggregation", userAggregation);
        ReflectionTestUtils.setField(scheduledAnalyticsJob, "thresholdAlertService", thresholdAlertService);
        ReflectionTestUtils.setField(scheduledAnalyticsJob, "mlPredictionService", mlPredictionService);
    }

    @Test
    @DisplayName("runAnalytics triggers every analytics collaborator")
    void runAnalyticsInvokesCollaborators() {
        assertThatCode(() -> scheduledAnalyticsJob.runAnalytics()).doesNotThrowAnyException();

        verify(incidentAggregation).compute();
        verify(vehicleAggregation).compute();
        verify(ticketAggregation).compute();
        verify(revenueAggregation).compute();
        verify(subscriptionAggregation).compute();
        verify(userAggregation).compute();
        verify(thresholdAlertService).detect();
        verify(mlPredictionService).computePeakHoursPrediction();
        verify(mlPredictionService).computeIncidentPrediction();
    }
}
