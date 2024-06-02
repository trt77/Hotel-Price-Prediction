package pl.optibooking.hotelpriceprediction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class HotelPricePredictionApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelPricePredictionApplication.class, args);
    }
}
