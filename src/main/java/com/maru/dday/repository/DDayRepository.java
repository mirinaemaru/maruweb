package com.maru.dday.repository;

import com.maru.dday.entity.DDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DDayRepository extends JpaRepository<DDay, Long> {

    List<DDay> findByDeletedOrderByTargetDateAsc(String deleted);

    List<DDay> findByDeletedAndTargetDateGreaterThanEqualOrderByTargetDateAsc(String deleted, LocalDate date);

    List<DDay> findByDeletedAndTargetDateLessThanOrderByTargetDateDesc(String deleted, LocalDate date);
}
