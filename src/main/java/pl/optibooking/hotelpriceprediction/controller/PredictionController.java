package pl.optibooking.hotelpriceprediction.controller;

import pl.optibooking.hotelpriceprediction.service.AsyncCSVService;
import pl.optibooking.hotelpriceprediction.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Controller
public class PredictionController {

    private static final Logger LOGGER = Logger.getLogger(PredictionController.class.getName());

    private final PredictionService predictionService;
    private final AsyncCSVService asyncCSVService;

    @Autowired
    public PredictionController(PredictionService predictionService, AsyncCSVService asyncCSVService) {
        this.asyncCSVService = asyncCSVService;
        this.predictionService = predictionService;
    }

    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

    @PostMapping("/upload")
    @ResponseBody
    public CompletableFuture<String> uploadCSV(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return CompletableFuture.completedFuture("Please select a CSV file to upload.");
        }

        return CompletableFuture.supplyAsync(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                List<String> lines = br.lines().collect(Collectors.toList());
                asyncCSVService.uploadCSVAsync(lines);
                return "Dataset uploading....";
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing CSV file", e);
                return "An error occurred while processing the CSV file.";
            }
        });
    }

    @GetMapping("/uploadStatus")
    @ResponseBody
    public String getUploadStatus() {
        if (asyncCSVService.isProcessing()) {
            return "Processing";
        } else {
            return "Processed";
        }
    }

    @PostMapping("/setTotalRooms")
    @ResponseBody
    public CompletableFuture<String> setTotalRooms(@RequestBody Map<String, Integer> totalRoomsByType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                predictionService.setTotalRoomsByType(totalRoomsByType);
                LOGGER.info("Total number of rooms per room type correctly persisted: " + totalRoomsByType);
                return "Total rooms set successfully.";
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error setting total rooms", e);
                return "An error occurred while setting total rooms.";
            }
        });
    }


    private volatile boolean isPredicting = false;

    @PostMapping("/predict")
    @ResponseBody
    public CompletableFuture<List<Double>> predictPrice(@RequestParam("startDate") String startDate,
                                                        @RequestParam("endDate") String endDate,
                                                        @RequestParam("roomType") String roomType,
                                                        @RequestParam("persons") int persons,
                                                        @RequestParam("occupancyRate") double occupancyRate) {
        LOGGER.info("Expected occupancy has been set to: " + occupancyRate + "%");
        isPredicting = true;
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Double> predictedPrices = predictionService.predictPrice(LocalDate.parse(startDate), LocalDate.parse(endDate), roomType, persons, occupancyRate);
                return predictedPrices;
            } catch (Exception e) {
                throw new RuntimeException("An error occurred while predicting the price.", e);
            } finally {
                isPredicting = false;
            }
        });
    }

    @GetMapping("/predictStatus")
    @ResponseBody
    public boolean getPredictStatus() {
        return isPredicting;
    }

    @GetMapping("/roomTypes")
    @ResponseBody
    public CompletableFuture<Set<String>> getRoomTypes() {
        LOGGER.info("Fetching room types");
        return CompletableFuture.supplyAsync(() -> predictionService.getRoomTypes());
    }

}
