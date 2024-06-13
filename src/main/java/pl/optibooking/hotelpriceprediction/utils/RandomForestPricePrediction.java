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
import java.util.logging.Logger;
import java.util.stream.Collectors;

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

        Instances dataSet = new Instances("StayData", attributes, stayDataList.size());
        dataSet.setClassIndex(attributes.size() - 1);

        for (StayData stayData : stayDataList) {
            double[] values = new double[attributes.size()];
            values[0] = stayData.getBeginOfStay().toEpochDay();
            values[1] = stayData.getEndOfStay().toEpochDay();
            values[2] = stayData.getPersons();
            values[3] = stayData.getOccupiedRooms();
            values[4] = stayData.getTotalRooms();
            values[5] = stayData.getTotalPrice();
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

    public static double predict(RandomForest model, List<StayData> stayDataList, LocalDate startDate, LocalDate endDate, String roomType, int persons, double expectedOccupancyRate, int totalRooms) {
        LOGGER.info("Starting prediction for date: " + startDate);

        double averageAnnualPriceIncrease = calculateAnnualPriceIncrease(stayDataList, roomType, persons);

        ArrayList<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("beginOfStay"));
        attributes.add(new Attribute("endOfStay"));
        attributes.add(new Attribute("persons"));
        attributes.add(new Attribute("occupiedRooms"));
        attributes.add(new Attribute("totalRooms"));
        attributes.add(new Attribute("totalPrice"));

        Instances dataSet = new Instances("StayData", attributes, 0);
        dataSet.setClassIndex(attributes.size() - 1);

        double[] values = new double[attributes.size()];
        values[0] = startDate.toEpochDay();
        values[1] = endDate.toEpochDay();
        values[2] = persons;
        values[3] = expectedOccupancyRate * totalRooms;
        values[4] = totalRooms;

        Instance instance = new DenseInstance(1.0, values);
        instance.setDataset(dataSet);

        double basePredictedPrice;
        try {
            basePredictedPrice = model.classifyInstance(instance);
            LOGGER.info("Base prediction completed. Predicted price: " + basePredictedPrice);
        } catch (Exception e) {
            LOGGER.severe("Failed to classify instance: " + e.getMessage());
            throw new RuntimeException(e);
        }

        double adjustedPrice = adjustPriceForOccupancy(basePredictedPrice, expectedOccupancyRate, totalRooms, stayDataList);
        LOGGER.info("Adjusted price: " + adjustedPrice);

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

    private static double adjustPriceForOccupancy(double basePrice, double expectedOccupancyRate, int totalRooms, List<StayData> stayDataList) {
        double averageOccupancyRate = stayDataList.stream()
                .mapToDouble(stay -> (double) stay.getOccupiedRooms() / stay.getTotalRooms())
                .average()
                .orElse(0.7);

        double occupancyAdjustmentFactor = 1.0;
        if (expectedOccupancyRate > averageOccupancyRate) {
            occupancyAdjustmentFactor = 1 + (expectedOccupancyRate - averageOccupancyRate) / averageOccupancyRate;
        } else if (expectedOccupancyRate < averageOccupancyRate) {
            occupancyAdjustmentFactor = 1 - (averageOccupancyRate - expectedOccupancyRate) / averageOccupancyRate;
        }

        double adjustedPrice = basePrice * occupancyAdjustmentFactor;

        LOGGER.info("Base price: " + basePrice + ", Adjusted price: " + adjustedPrice + ", Occupancy Adjustment Factor: " + occupancyAdjustmentFactor);

        return adjustedPrice;
    }
}
