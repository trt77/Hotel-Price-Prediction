package pl.optibooking.hotelpriceprediction.service;

import pl.optibooking.hotelpriceprediction.model.StayData;
import pl.optibooking.hotelpriceprediction.repository.StayDataRepository;
import pl.optibooking.hotelpriceprediction.utils.RandomForestPricePrediction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class PredictionService {

    private static final Logger LOGGER = Logger.getLogger(PredictionService.class.getName());

    @Autowired
    private StayDataRepository stayDataRepository;

    private Set<String> roomTypes = new HashSet<>();
    private Map<String, Integer> totalRoomsByType = new HashMap<>();

    // Metoda do przetwarzania CSV i zapisywania danych do bazy
    public void uploadCSV(List<String> lines) {
        try {
            // Pomijamy nagłówek
            if (!lines.isEmpty()) {
                lines.remove(0);
            }
            // Przetwarzamy linie danych
            for (String line : lines) {
                String[] values = line.split(",");
                StayData stayData = new StayData();
                stayData.setBeginOfStay(LocalDate.parse(values[1]));
                stayData.setEndOfStay(LocalDate.parse(values[2]));
                stayData.setPersons(Integer.parseInt(values[3]));
                stayData.setRoomType(values[4]);
                stayData.setTotalPrice(Double.parseDouble(values[5]));
                stayDataRepository.save(stayData);
                roomTypes.add(values[4]);
            }
            LOGGER.info("CSV file processed successfully. Room types: " + roomTypes);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing CSV file", e);
        }
    }

    // Metoda do przewidywania cen
    public List<Double> predictPrice(LocalDate startDate, LocalDate endDate, String roomType, int persons, double occupancyRate) {
        int totalRooms = totalRoomsByType.getOrDefault(roomType, 0);
        LOGGER.info("Predicting prices for the period from " + startDate + " to " + endDate + " for room type " + roomType + " with total rooms " + totalRooms);
        List<StayData> stayDataList = stayDataRepository.findAll();
        List<LocalDate> dates = startDate.datesUntil(endDate.plusDays(1)).collect(Collectors.toList());
        List<Double> predictedPrices = dates.stream()
                .map(date -> {
                    double price = RandomForestPricePrediction.predict(stayDataList, date, date, roomType, persons, occupancyRate, totalRooms);
                    LOGGER.info("Predicted price for " + date + ": " + price);
                    return price;
                })
                .collect(Collectors.toList());
        LOGGER.info("Prediction results: " + predictedPrices);
        return predictedPrices;
    }

    // Metoda do zwracania dostępnych typów pokoi
    public Set<String> getRoomTypes() {
        LOGGER.info("Returning room types: " + roomTypes);
        return roomTypes;
    }

    // Metoda do ustawiania liczby pokoi według typu
    public void setTotalRoomsByType(Map<String, Integer> totalRoomsByType) {
        this.totalRoomsByType = totalRoomsByType;
    }
}
