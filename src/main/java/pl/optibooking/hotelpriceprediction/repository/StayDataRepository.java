package pl.optibooking.hotelpriceprediction.repository;

import pl.optibooking.hotelpriceprediction.model.StayData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StayDataRepository extends JpaRepository<StayData, Long> {
}