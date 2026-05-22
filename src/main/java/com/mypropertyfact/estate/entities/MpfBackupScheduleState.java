package com.mypropertyfact.estate.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "mpf_backup_schedule_state")
@Getter
@Setter
@NoArgsConstructor
public class MpfBackupScheduleState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "go_live_at", nullable = false)
    private LocalDateTime goLiveAt;

    @Column(name = "first_eligible_backup_at", nullable = false)
    private LocalDateTime firstEligibleBackupAt;
}
