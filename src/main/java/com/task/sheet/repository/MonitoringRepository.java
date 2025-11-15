package com.task.sheet.repository;

import com.task.sheet.model.Monitoring;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Repository
public interface MonitoringRepository extends JpaRepository<Monitoring, Integer> {
    List<Monitoring> findByLoginTime(LocalDateTime startDate);

    List<Monitoring> findByLoginTimeBetween(LocalDateTime startDate, LocalDateTime endDate);
}
