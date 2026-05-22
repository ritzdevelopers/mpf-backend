package com.mypropertyfact.estate.services;

import com.mypropertyfact.estate.entities.MpfBackupScheduleState;
import com.mypropertyfact.estate.repositories.MpfBackupScheduleStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
public class MpfBackupScheduleService {

    private final MpfBackupScheduleStateRepository scheduleStateRepository;

    @Transactional
    public LocalDateTime getOrInitFirstEligibleBackupAt() {
        return scheduleStateRepository.findAll().stream()
                .findFirst()
                .map(MpfBackupScheduleState::getFirstEligibleBackupAt)
                .orElseGet(this::initializeSchedule);
    }

    public boolean isEligibleForScheduledRun(LocalDateTime now) {
        LocalDateTime first = getOrInitFirstEligibleBackupAt();
        return !now.isBefore(first);
    }

    private LocalDateTime initializeSchedule() {
        LocalDateTime goLive = LocalDateTime.now();
        LocalDateTime firstEligible = computeFirstEligibleMonday2Pm(goLive);
        MpfBackupScheduleState state = new MpfBackupScheduleState();
        state.setGoLiveAt(goLive);
        state.setFirstEligibleBackupAt(firstEligible);
        scheduleStateRepository.save(state);
        return firstEligible;
    }

    /**
     * Go-live on Monday → first backup the following Monday at 14:00.
     * Go-live any other day → first Monday after go-live at 14:00.
     */
    static LocalDateTime computeFirstEligibleMonday2Pm(LocalDateTime goLive) {
        LocalDate goLiveDate = goLive.toLocalDate();
        LocalDate nextMonday = goLiveDate.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        if (goLiveDate.getDayOfWeek() == DayOfWeek.MONDAY) {
            nextMonday = goLiveDate.plusWeeks(1);
        }
        return LocalDateTime.of(nextMonday, LocalTime.of(14, 0));
    }
}
