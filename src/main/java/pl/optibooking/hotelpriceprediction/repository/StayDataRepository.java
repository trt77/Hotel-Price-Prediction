package pl.optibooking.hotelpriceprediction.repository;

import pl.optibooking.hotelpriceprediction.model.StayData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StayDataRepository extends JpaRepository<StayData, Long> {
    List<StayData> findAll();
}
