package com.g7suivivehicules.dto;


import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionGPSResponse {

    private UUID id;
    private UUID vehiculeId;
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;
    private Double vitesse;
    private Double cap;
}