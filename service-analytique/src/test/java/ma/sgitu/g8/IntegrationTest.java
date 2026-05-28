package ma.sgitu.g8;

import ma.sgitu.g8.ingestion.IngestionService;
import ma.sgitu.g8.ingestion.dto.BatchIngestionResponse;
import ma.sgitu.g8.model.Report;
import ma.sgitu.g8.model.SnapshotType;
import ma.sgitu.g8.model.SourceType;
import ma.sgitu.g8.model.StatSnapshot;
import ma.sgitu.g8.repository.EventRepository;
import ma.sgitu.g8.repository.ReportRepository;
import ma.sgitu.g8.repository.StatSnapshotRepository;
import ma.sgitu.g8.service.AnalyticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private StatSnapshotRepository statSnapshotRepository;

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private IngestionService ingestionService;

    @Test
    @DisplayName("IngestionService maps a valid ticket event into a persisted incoming event")
    void ingestionMapsAndPersistsEvent() {
        BatchIngestionResponse response = ingestionService.ingest(List.of(Map.of(
                "timestamp", "2026-05-05T11:00:00Z",
                "userId", "user-1",
                "status", "validated",
                "line", "L1"
        )), SourceType.TICKETING);

        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getTotalAccepted()).isEqualTo(1);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(eventRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
    }

    @Test
    @DisplayName("AnalyticsService generates a report from non-prediction snapshots only")
    void analyticsServiceBuildsReportFromSnapshots() {
        AnalyticsService analyticsService = new AnalyticsService(statSnapshotRepository, reportRepository);

        StatSnapshot actualSnapshot = StatSnapshot.builder()
                .id("snap-1")
                .snapshotType(SnapshotType.TRIPS)
                .statId("FREQ_01")
                .computedAt(LocalDateTime.now())
                .isPrediction(false)
                .build();
        StatSnapshot predictionSnapshot = StatSnapshot.builder()
                .id("snap-2")
                .snapshotType(SnapshotType.TRIPS)
                .statId("PRED_01")
                .computedAt(LocalDateTime.now())
                .isPrediction(true)
                .build();

        when(statSnapshotRepository.findBySnapshotType(SnapshotType.TRIPS))
                .thenReturn(List.of(actualSnapshot, predictionSnapshot));
        when(reportRepository.save(org.mockito.ArgumentMatchers.any(Report.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Report report = analyticsService.generateReport("2026-05-05", List.of(SnapshotType.TRIPS));

        assertThat(report.getPeriod()).isEqualTo("2026-05-05");
        assertThat(report.getSnapshots()).containsExactly(actualSnapshot);
    }

    @Test
    @DisplayName("AnalyticsService returns a report when the repository finds one")
    void analyticsServiceReturnsReportById() {
        AnalyticsService analyticsService = new AnalyticsService(statSnapshotRepository, reportRepository);
        Report report = Report.builder().id("report-1").period("2026-05-05").build();

        when(reportRepository.findById(anyString())).thenReturn(Optional.of(report));

        assertThat(analyticsService.getReportById("report-1")).contains(report);
    }
}
