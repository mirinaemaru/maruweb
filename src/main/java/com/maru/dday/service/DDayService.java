package com.maru.dday.service;

import com.maru.dday.entity.DDay;
import com.maru.dday.repository.DDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DDayService {

    private final DDayRepository ddayRepository;

    public List<DDay> getAllDDays() {
        return ddayRepository.findByDeletedOrderByTargetDateAsc("N");
    }

    public List<DDay> getUpcomingDDays() {
        return ddayRepository.findByDeletedAndTargetDateGreaterThanEqualOrderByTargetDateAsc("N", LocalDate.now());
    }

    public List<DDay> getPastDDays() {
        return ddayRepository.findByDeletedAndTargetDateLessThanOrderByTargetDateDesc("N", LocalDate.now());
    }

    public Optional<DDay> getDDayById(Long id) {
        return ddayRepository.findById(id)
                .filter(dday -> "N".equals(dday.getDeleted()));
    }

    @Transactional
    public DDay createDDay(DDay dday) {
        return ddayRepository.save(dday);
    }

    @Transactional
    public DDay updateDDay(Long id, DDay updatedDDay) {
        return ddayRepository.findById(id)
                .filter(dday -> "N".equals(dday.getDeleted()))
                .map(dday -> {
                    dday.setTitle(updatedDDay.getTitle());
                    dday.setDescription(updatedDDay.getDescription());
                    dday.setTargetDate(updatedDDay.getTargetDate());
                    dday.setIcon(updatedDDay.getIcon());
                    return ddayRepository.save(dday);
                })
                .orElseThrow(() -> new IllegalArgumentException("D-Day not found with id: " + id));
    }

    @Transactional
    public void deleteDDay(Long id) {
        ddayRepository.findById(id)
                .filter(dday -> "N".equals(dday.getDeleted()))
                .ifPresent(dday -> {
                    dday.setDeleted("Y");
                    ddayRepository.save(dday);
                });
    }
}
