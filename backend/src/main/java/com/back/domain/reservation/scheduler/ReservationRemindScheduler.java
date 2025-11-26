package com.back.domain.reservation.scheduler;

import com.back.domain.reservation.scheduler.job.ReservationReturnRemindJob;
import com.back.standard.util.quartz.QuartzUt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationRemindScheduler {

    private final Scheduler scheduler;

    public void scheduleReturnReminder(Long reservationId, LocalDateTime reservationEndAt) {

        log.info("[SCHEDULER] 반납 리마인더 스케줄링 요청 - reservationId: {}", reservationId);

        JobDetail jobDetail = JobBuilder.newJob(ReservationReturnRemindJob.class)
                .withIdentity("returnReminderJob-" + reservationId)
                .usingJobData("reservationId", reservationId)
                .build();

        LocalDateTime runAt = QuartzUt.reminderAt10AmOneDayBefore(reservationEndAt);

        if (runAt.isBefore(LocalDateTime.now())) {
            log.warn("알림 시간이 과거입니다 - 1분 뒤 실행으로 변경 - reservationId: {}, runAt: {}",
                    reservationId, runAt);
            runAt = LocalDateTime.now().plusMinutes(1);
        }

        Date runDate = Date.from(runAt.atZone(ZoneId.systemDefault()).toInstant());

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("returnReminderTrigger-" + reservationId)
                .startAt(runDate)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withMisfireHandlingInstructionFireNow())
                .forJob(jobDetail)
                .build();

        try {
            scheduler.scheduleJob(jobDetail, trigger);
            log.info("[SCHEDULER] 반납 리마인더 스케줄링 완료 - reservationId: {}, runAt: {}", reservationId, runAt);
        } catch (SchedulerException e) {
            log.error("[SCHEDULER] 반납 리마인더 스케줄링 실패 - reservationId: {}, runAt: {}", reservationId, runAt, e);
        }
    }
}

