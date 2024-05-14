package pl.optibooking.hotelpriceprediction.controller;

import pl.optibooking.hotelpriceprediction.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
public class PredictionController {

    @Autowired
    private PredictionService predictionService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/upload")
    public String uploadCSV(@RequestParam("file") String filePath) {
        predictionService.uploadCSV(filePath);
        return "redirect:/";
    }

    @PostMapping("/predict")
    public String predictPrice(@RequestParam("startDate") String startDate,
                               @RequestParam("endDate") String endDate,
                               @RequestParam("roomType") String roomType,
                               @RequestParam("persons") int persons,
                               @RequestParam("occupancyRate") double occupancyRate,
                               Model model) {
        double predictedPrice = predictionService.predictPrice(LocalDate.parse(startDate), LocalDate.parse(endDate), roomType, persons, occupancyRate);
        model.addAttribute("predictedPrice", predictedPrice);
        return "index";
    }
}
