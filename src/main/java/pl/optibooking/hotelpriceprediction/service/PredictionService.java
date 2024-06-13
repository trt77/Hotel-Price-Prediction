package pl.optibooking.hotelpriceprediction.service;

import pl.optibooking.hotelpriceprediction.model.StayData;
import pl.optibooking.hotelpriceprediction.repository.StayDataRepository;
import pl.optibooking.hotelpriceprediction.utils.RandomForestPricePrediction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import weka.classifiers.trees.RandomForest;
import weka.core.SerializationHelper;

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

    private final Set<String> roomTypes = new HashSet<>();
    private Map<String, Integer> totalRoomsByType = new HashMap<>();
    private static RandomForest model;
    public static final String MODEL_PATH;

    static {
        MODEL_PATH = "hotel_price_model.model";
    }

    public void uploadCSV(List<String> lines) {
        try {
            if (!lines.isEmpty()) {
                lines.remove(0); // Remove header
            }
            for (String line : lines) {
                String[] values = line.split(",");
                StayData stayData = new StayData();
                stayData.setBeginOfStay(LocalDate.parse(values[1]));
                stayData.setEndOfStay(LocalDate.parse(values[2]));
                stayData.setPersons(Integer.parseInt(values[3]));
                stayData.setRoomType(values[4]);
                stayData.setTotalPrice(Double.parseDouble(values[5]));
                // TotalRooms and OccupiedRooms are not in the CSV, calculate later
                stayDataRepository.save(stayData);
                roomTypes.add(values[4]);
                LOGGER.info("Processed StayData: " + stayData);
            }
            LOGGER.info("CSV file processed successfully. Room types: " + roomTypes);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing CSV file", e);
        }
    }

    public RandomForest trainModel() {
        List<StayData> stayDataList = stayDataRepository.findAll();
        RandomForest model = RandomForestPricePrediction.train(stayDataList);
        PredictionService.model = model;
        try {
            SerializationHelper.write(MODEL_PATH, model);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving model", e);
        }
        return model;
    }

    public List<Double> predictPrice(LocalDate startDate, LocalDate endDate, String roomType, int persons, double occupancyRate) {
        if (model == null) {
            try {
                model = (RandomForest) SerializationHelper.read(MODEL_PATH);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error loading model", e);
                throw new RuntimeException("Model not trained or found");
            }
        }

        LOGGER.info("Predicting prices for the period from " + startDate + " to " + endDate + " for room type " + roomType + " with expected occupancy " + occupancyRate + "%");
        List<StayData> stayDataList = stayDataRepository.findAll();
        LOGGER.info("Total StayData entries: " + stayDataList.size());

        List<LocalDate> dates = startDate.datesUntil(endDate.plusDays(1)).collect(Collectors.toList());
        LOGGER.info("Total dates for prediction: " + dates.size());

        List<Double> predictedPrices = dates.stream()
                .map(date -> {
                    LOGGER.info("Running prediction for date: " + date);
                    double price = RandomForestPricePrediction.predict(model, stayDataList, date, roomType, persons, occupancyRate / 100.0, totalRoomsByType);
                    LOGGER.info("Predicted price for " + date + ": " + price);
                    return price;
                })
                .collect(Collectors.toList());
        LOGGER.info("Prediction results: " + predictedPrices);
        return predictedPrices;
    }



    public Set<String> getRoomTypes() {
        LOGGER.info("Returning room types: " + roomTypes);
        return roomTypes;
    }

    public void setTotalRoomsByType(Map<String, Integer> totalRoomsByType) {
        this.totalRoomsByType = totalRoomsByType;
        LOGGER.info("Total number of rooms per room type correctly persisted: " + totalRoomsByType);
    }
}
