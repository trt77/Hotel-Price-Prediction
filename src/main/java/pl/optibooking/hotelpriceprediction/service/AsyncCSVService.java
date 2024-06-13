package pl.optibooking.hotelpriceprediction.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pl.optibooking.hotelpriceprediction.model.StayData;
import pl.optibooking.hotelpriceprediction.repository.StayDataRepository;
import weka.classifiers.trees.RandomForest;
import weka.core.SerializationHelper;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class AsyncCSVService {

    private static final Logger LOGGER = Logger.getLogger(AsyncCSVService.class.getName());

    @Autowired
    private StayDataRepository stayDataRepository;

    @Autowired
    private PredictionService predictionService;

    private AtomicBoolean isProcessing = new AtomicBoolean(false);

    @Async
    public void uploadCSVAsync(List<String> lines) {
        isProcessing.set(true);
        try {
            predictionService.uploadCSV(lines);
            LOGGER.info("CSV file processed successfully.");

            // Train the model and save it
            RandomForest model = predictionService.trainModel();
            SerializationHelper.write("hotel_price_model.model", model);
            LOGGER.info("Model trained and saved successfully.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing CSV file", e);
        } finally {
            isProcessing.set(false);
        }
    }

    public boolean isProcessing() {
        return isProcessing.get();
    }
}
