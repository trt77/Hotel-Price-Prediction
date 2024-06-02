package pl.optibooking.hotelpriceprediction.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class AsyncCSVService {

    private static final Logger LOGGER = Logger.getLogger(AsyncCSVService.class.getName());

    @Autowired
    private PredictionService predictionService;

    private AtomicBoolean isProcessing = new AtomicBoolean(false);

    @Async
    public void uploadCSVAsync(List<String> lines) {
        isProcessing.set(true);
        try {
            predictionService.uploadCSV(lines);
            LOGGER.info("CSV file processed successfully.");
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
