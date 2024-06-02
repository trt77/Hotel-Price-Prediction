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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Controller
public class PredictionController {

    private static final Logger LOGGER = Logger.getLogger(PredictionController.class.getName());

    private final PredictionService predictionService;
    private final AsyncCSVService asyncCSVService;

    @Autowired
    public PredictionController(PredictionService predictionService,AsyncCSVService asyncCSVService) {
        this.asyncCSVService = asyncCSVService;
        this.predictionService = predictionService;
    }

    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

    @PostMapping("/upload")
    @ResponseBody
    public String uploadCSV(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "Please select a CSV file to upload.";
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            List<String> lines = br.lines().collect(Collectors.toList());
            asyncCSVService.uploadCSVAsync(lines);
            return "Dataset uploading....";
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing CSV file", e);
            return "An error occurred while processing the CSV file.";
        }
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
    public String setTotalRooms(@RequestParam Map<String, String> totalRoomsByType) {
        try {
            Map<String, Integer> totalRooms = totalRoomsByType.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> Integer.parseInt(entry.getValue())));
            predictionService.setTotalRoomsByType(totalRooms);
            return "Total rooms set successfully.";
        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Error parsing total rooms", e);
            return "An error occurred while setting total rooms.";
        }
    }

    @PostMapping("/predict")
    @ResponseBody
    public List<Double> predictPrice(@RequestParam("startDate") String startDate,
                                     @RequestParam("endDate") String endDate,
                                     @RequestParam("roomType") String roomType,
                                     @RequestParam("persons") int persons,
                                     @RequestParam("occupancyRate") double occupancyRate) {
        try {
            LOGGER.info("Predicting prices from " + startDate + " to " + endDate + " for room type " + roomType);
            List<Double> predictedPrices = predictionService.predictPrice(LocalDate.parse(startDate), LocalDate.parse(endDate), roomType, persons, occupancyRate);
            LOGGER.info("Prediction completed: " + predictedPrices);
            return predictedPrices;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error predicting price", e);
            throw new RuntimeException("An error occurred while predicting the price.");
        }
    }

    @GetMapping("/roomTypes")
    @ResponseBody
    public Set<String> getRoomTypes() {
        LOGGER.info("Fetching room types");
        return predictionService.getRoomTypes();
    }
}
