package pl.optibooking.hotelpriceprediction.utils;

import pl.optibooking.hotelpriceprediction.model.StayData;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static pl.optibooking.hotelpriceprediction.service.PredictionService.MODEL_PATH;

public class RandomForestPricePrediction {

    private static final Logger LOGGER = Logger.getLogger(RandomForestPricePrediction.class.getName());

    public static RandomForest train(List<StayData> stayDataList) {
        LOGGER.info("Starting model training.");

        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("beginOfStay"));
        attributes.add(new Attribute("endOfStay"));
        attributes.add(new Attribute("persons"));
        attributes.add(new Attribute("occupiedRooms"));
        attributes.add(new Attribute("totalRooms"));
        attributes.add(new Attribute("totalPrice"));
        attributes.add(new Attribute("dayOfWeek"));
        attributes.add(new Attribute("month"));

        Instances dataSet = new Instances("StayData", attributes, stayDataList.size());
        dataSet.setClassIndex(attributes.size() - 1);

        for (StayData stayData : stayDataList) {
            double[] values = new double[attributes.size()];
            values[0] = stayData.getBeginOfStay().toEpochDay();
            values[1] = stayData.getEndOfStay().toEpochDay();
            values[2] = stayData.getPersons();
            values[3] = calculateOccupiedRooms(stayDataList, stayData.getBeginOfStay(), stayData.getRoomType());
            values[4] = stayData.getTotalRooms();
            values[5] = stayData.getTotalPrice();
            values[6] = stayData.getBeginOfStay().getDayOfWeek().getValue();
            values[7] = stayData.getBeginOfStay().getMonthValue();
            dataSet.add(new DenseInstance(1.0, values));
        }

        LOGGER.info("Data set prepared with " + dataSet.numInstances() + " instances.");

        RandomForest randomForest = new RandomForest();
        randomForest.setNumExecutionSlots(Runtime.getRuntime().availableProcessors());

        try {
            randomForest.buildClassifier(dataSet);
            LOGGER.info("Random Forest model built successfully.");
        } catch (Exception e) {
            LOGGER.severe("Failed to build Random Forest model: " + e.getMessage());
            throw new RuntimeException(e);
        }

        return randomForest;
    }

    public static double predict(RandomForest model, List<StayData> stayDataList, LocalDate date, String roomType, int persons, double expectedOccupancyRate, Map<String, Integer> totalRoomsByType) {
        LOGGER.info("Starting prediction for date: " + date);

        double averagePrice = calculateWeightedAveragePrice(stayDataList, date, roomType, persons);
        double averageOccupancyRate = calculateAverageOccupancyRate(stayDataList, date, roomType, totalRoomsByType);

        double adjustedPriceForInflation = adjustPriceForInflation(averagePrice, date, stayDataList, roomType, persons);
        double intermediatePrice = adjustPriceForOccupancy(adjustedPriceForInflation, expectedOccupancyRate, averageOccupancyRate);

        // Create a new instance for prediction
        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("beginOfStay"));
        attributes.add(new Attribute("endOfStay"));
        attributes.add(new Attribute("persons"));
        attributes.add(new Attribute("occupiedRooms"));
        attributes.add(new Attribute("totalRooms"));
        attributes.add(new Attribute("totalPrice"));
        attributes.add(new Attribute("dayOfWeek"));
        attributes.add(new Attribute("month"));

        Instances dataSet = new Instances("PredictData", attributes, 0);
        dataSet.setClassIndex(attributes.size() - 1);

        double[] values = new double[attributes.size()];
        values[0] = date.toEpochDay();
        values[1] = date.toEpochDay();
        values[2] = persons;
        values[3] = calculateOccupiedRooms(stayDataList, date, roomType); // Dynamically calculate
        values[4] = totalRoomsByType.get(roomType);
        values[5] = intermediatePrice; // Use the intermediate price as the initial guess
        values[6] = date.getDayOfWeek().getValue();
        values[7] = date.getMonthValue();

        Instance newInstance = new DenseInstance(1.0, values);
        newInstance.setDataset(dataSet);

        try {
            double modelPredictedPrice = model.classifyInstance(newInstance);
            LOGGER.info("Model predicted price for date " + date + ": " + modelPredictedPrice);
            return modelPredictedPrice;
        } catch (Exception e) {
            LOGGER.severe("Failed to predict price: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }







    private static double calculateWeightedAveragePrice(List<StayData> stayDataList, LocalDate date, String roomType, int persons) {
        List<Double> exactDayPrices = new ArrayList<>();
        List<Double> surroundingDayPrices = new ArrayList<>();

        LOGGER.info("Calculating weighted average price for date: " + date + " with room type: " + roomType + " and persons: " + persons);

        for (int i = -4; i <= 4; i++) {
            LocalDate targetDate = date.plusDays(i);
            List<StayData> filteredData = stayDataList.stream()
                    .filter(stay -> stay.getRoomType().equals(roomType))
                    .filter(stay -> stay.getPersons() == persons)
                    .filter(stay -> !stay.getBeginOfStay().isAfter(targetDate) && !stay.getEndOfStay().isBefore(targetDate))
                    .collect(Collectors.toList());

            LOGGER.info("Target date: " + targetDate + ", Filtered data size: " + filteredData.size());

            for (StayData stay : filteredData) {
                long days = stay.getBeginOfStay().until(stay.getEndOfStay()).getDays() + 1;
                double dailyPrice = stay.getTotalPrice() / days;
                if (i == 0) {
                    exactDayPrices.add(dailyPrice);
                } else {
                    surroundingDayPrices.add(dailyPrice);
                }
            }
        }

        double exactDayAverage = exactDayPrices.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double surroundingDaysAverage = surroundingDayPrices.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        double weightedAveragePrice = (0.7 * exactDayAverage) + (0.3 * surroundingDaysAverage);

        LOGGER.info("Exact day prices: " + exactDayPrices);
        LOGGER.info("Surrounding days prices: " + surroundingDayPrices);
        LOGGER.info("Exact day average price: " + exactDayAverage);
        LOGGER.info("Surrounding days average price: " + surroundingDaysAverage);
        LOGGER.info("Weighted average price for room type " + roomType + " with " + persons + " persons on date " + date + ": " + weightedAveragePrice);

        return weightedAveragePrice;
    }



    private static double calculateAverageOccupancyRate(List<StayData> stayDataList, LocalDate date, String roomType, Map<String, Integer> totalRoomsByType) {
        List<Double> occupancies = new ArrayList<>();
        for (int i = -4; i <= 4; i++) {
            LocalDate targetDate = date.plusDays(i);
            long occupiedRooms = stayDataList.stream()
                    .filter(stay -> stay.getRoomType().equals(roomType))
                    .filter(stay -> !stay.getBeginOfStay().isAfter(targetDate) && !stay.getEndOfStay().isBefore(targetDate))
                    .count();

            int totalRooms = totalRoomsByType.getOrDefault(roomType, 1);
            occupancies.add((double) occupiedRooms / totalRooms);
        }
        double averageOccupancyRate = occupancies.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        LOGGER.info("Average historical occupancy for room type " + roomType + " on date " + date + " (9-day window): " + averageOccupancyRate);
        return averageOccupancyRate;
    }



    private static double adjustPriceForInflation(double basePrice, LocalDate date, List<StayData> stayDataList, String roomType, int persons) {
        double averageAnnualPriceIncrease = calculateAnnualPriceIncrease(stayDataList, roomType, persons);

        double currentYear = date.getYear();
        double initialYear = stayDataList.stream().mapToInt(stay -> stay.getBeginOfStay().getYear()).min().orElse((int) currentYear);
        double yearsDifference = currentYear - initialYear;
        double adjustedPrice = basePrice * Math.pow((1 + averageAnnualPriceIncrease), yearsDifference);

        LOGGER.info("Average yearly price increase for room type " + roomType + " with " + persons + " persons: " + averageAnnualPriceIncrease);
        LOGGER.info("Price adjusted for inflation increased by " + (adjustedPrice - basePrice) / basePrice * 100 + "%");

        return adjustedPrice;
    }

    private static double calculateAnnualPriceIncrease(List<StayData> stayDataList, String roomType, int persons) {
        List<StayData> filteredData = stayDataList.stream()
                .filter(stay -> stay.getRoomType().equals(roomType) && stay.getPersons() == persons)
                .collect(Collectors.toList());

        double totalPriceChange = 0.0;
        int count = 0;

        for (int year = LocalDate.now().getYear() - 1; year >= LocalDate.now().getYear() - 5; year--) {
            int finalYear = year;
            double previousYearPrice = filteredData.stream()
                    .filter(stay -> stay.getBeginOfStay().getYear() == finalYear)
                    .mapToDouble(StayData::getTotalPrice)
                    .average()
                    .orElse(0.0);

            int finalYear1 = year;
            double currentYearPrice = filteredData.stream()
                    .filter(stay -> stay.getBeginOfStay().getYear() == finalYear1 + 1)
                    .mapToDouble(StayData::getTotalPrice)
                    .average()
                    .orElse(0.0);

            if (previousYearPrice > 0 && currentYearPrice > 0) {
                totalPriceChange += (currentYearPrice - previousYearPrice) / previousYearPrice;
                count++;
            }
        }

        return count > 0 ? totalPriceChange / count : 0.0;
    }

    private static double adjustPriceForOccupancy(double basePrice, double expectedOccupancyRate, double averageOccupancyRate) {
        double occupancyAdjustmentFactor = 1.0;
        if (averageOccupancyRate != 0) {
            occupancyAdjustmentFactor = 1 + (expectedOccupancyRate - averageOccupancyRate) / averageOccupancyRate;
        }

        double adjustedPrice = basePrice * occupancyAdjustmentFactor;
        LOGGER.info("Price adjusted for occupancy by " + (adjustedPrice - basePrice) / basePrice * 100 + "%");

        return adjustedPrice;
    }

    private static int calculateOccupiedRooms(List<StayData> stayDataList, LocalDate date, String roomType) {
        return (int) stayDataList.stream()
                .filter(stay -> stay.getRoomType().equals(roomType))
                .filter(stay -> !stay.getBeginOfStay().isAfter(date) && !stay.getEndOfStay().isBefore(date))
                .count();
    }

}
