package pl.optibooking.hotelpriceprediction.utils;

import pl.optibooking.hotelpriceprediction.model.StayData;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.time.LocalDate;
import java.util.List;

public class OccupancyPredictor {

    private OLSMultipleLinearRegression regressionModel;

    public void trainModel(List<StayData> stayDataList) {
        // Prepare the dataset for regression
        double[][] features = stayDataList.stream()
                .map(stay -> new double[]{stay.getTotalPrice(), stay.getPersons(), stay.getBeginOfStay().getDayOfYear()})
                .toArray(double[][]::new);

        double[] occupancyRates = stayDataList.stream()
                .mapToDouble(stay -> (double) stay.getOccupiedRooms() / stay.getTotalRooms())
                .toArray();

        regressionModel = new OLSMultipleLinearRegression();
        regressionModel.newSampleData(occupancyRates, features);
    }

    public double predictOccupancy(double price, int persons, LocalDate date) {
        double[] coefficients = regressionModel.estimateRegressionParameters();
        double intercept = coefficients[0];
        double[] slopes = new double[coefficients.length - 1];
        System.arraycopy(coefficients, 1, slopes, 0, slopes.length);

        double[] features = new double[]{price, persons, date.getDayOfYear()};
        double prediction = intercept;
        for (int i = 0; i < features.length; i++) {
            prediction += slopes[i] * features[i];
        }
        return prediction;
    }
}
