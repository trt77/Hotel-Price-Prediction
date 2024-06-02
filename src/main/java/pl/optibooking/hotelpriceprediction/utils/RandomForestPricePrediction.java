package pl.optibooking.hotelpriceprediction.utils;

// Importujemy klasę StayData, która reprezentuje dane o pobytach
import pl.optibooking.hotelpriceprediction.model.StayData;
// Importujemy klasę LocalDate z biblioteki Java do obsługi dat
import java.time.LocalDate;
// Importujemy listę, która będzie przechowywać dane o pobytach
import java.util.List;
// Importujemy logger do logowania informacji
import java.util.logging.Logger;
// Importujemy kolektory strumieniowe do przetwarzania danych
import java.util.stream.Collectors;

public class RandomForestPricePrediction {

    // Tworzymy logger do logowania informacji o predykcji
    private static final Logger LOGGER = Logger.getLogger(RandomForestPricePrediction.class.getName());

    // Główna metoda do predykcji cen na podstawie danych historycznych
    public static double predict(List<StayData> stayDataList, LocalDate startDate, LocalDate endDate, String roomType, int persons, double expectedOccupancyRate, int totalRooms) {
        // Logujemy rozpoczęcie procesu predykcji
        LOGGER.info("Starting prediction for date: " + startDate);

        // Filtrujemy dane według typu pokoju i liczby osób
        List<StayData> filteredData = stayDataList.stream()
                .filter(stay -> stay.getRoomType().equals(roomType) && stay.getPersons() == persons)
                .collect(Collectors.toList());

        // Logujemy liczbę przefiltrowanych pobytów
        LOGGER.info("Filtered stays: " + filteredData.size());

        // Ustalamy bieżący rok, miesiąc i dzień na podstawie startDate
        LocalDate currentYear = LocalDate.now().withMonth(startDate.getMonthValue()).withDayOfMonth(startDate.getDayOfMonth());
        LOGGER.info("Evaluating prices for the same day in the past years and surrounding days.");

        // Inicjalizujemy zmienne do przechowywania wcześniejszych cen, całkowitej zmiany cen i całkowitego obłożenia
        double previousYearPrice = 0.0;
        double totalPriceChange = 0.0;
        int count = 0;
        double totalOccupancyRate = 0.0;

        // Pętla przez ostatnie 5 lat danych historycznych
        for (int year = startDate.getYear() - 5; year < startDate.getYear(); year++) {
            // Ustalamy datę w przeszłości na podstawie bieżącego roku
            LocalDate pastDate = currentYear.withYear(year);
            // Filtrujemy dane z przeszłości na podstawie dat pobytu
            List<StayData> pastStays = filteredData.stream()
                    .filter(stay -> !stay.getEndOfStay().isBefore(pastDate.minusDays(4)) && !stay.getBeginOfStay().isAfter(pastDate.plusDays(4)))
                    .collect(Collectors.toList());

            // Sprawdzamy, czy mamy dane z przeszłości do analizy
            if (!pastStays.isEmpty()) {
                // Obliczamy średnią cenę za przeszłe pobyty
                double pastPrice = pastStays.stream().mapToDouble(StayData::getTotalPrice).average().orElse(0.0);
                LOGGER.info("Average price for " + pastDate + ": " + pastPrice);

                int totalOccupiedRooms = 0;
                int daysEvaluated = 0;

                // Pętla przez 9 dni wokół rozważanej daty
                for (int dayOffset = -4; dayOffset <= 4; dayOffset++) {
                    // Ustalamy datę do oceny na podstawie przesunięcia od daty przeszłej
                    LocalDate evalDate = pastDate.plusDays(dayOffset);
                    // Liczba zajętych pokoi w danym dniu
                    long occupiedRooms = pastStays.stream()
                            .filter(stay -> !stay.getEndOfStay().isBefore(evalDate) && !stay.getBeginOfStay().isAfter(evalDate))
                            .count();
                    totalOccupiedRooms += occupiedRooms;
                    daysEvaluated++;
                    LOGGER.info("Occupied rooms on " + evalDate + ": " + occupiedRooms);
                }

                // Obliczamy średnie obłożenie dla rozważanej daty
                double averageOccupiedRooms = (double) totalOccupiedRooms / daysEvaluated;
                double occupancy = (averageOccupiedRooms / totalRooms) * 100;  // Obliczanie obłożenia jako procent
                totalOccupancyRate += occupancy;
                LOGGER.info("Occupancy rate for " + pastDate + ": " + occupancy + "% (Average occupied rooms: " + averageOccupiedRooms + " over 9 days)");

                // Obliczamy zmianę ceny w stosunku do poprzedniego roku, jeśli mamy dane do porównania
                if (count > 0) {
                    double priceChange = (pastPrice - previousYearPrice) / previousYearPrice;
                    totalPriceChange += priceChange;
                    LOGGER.info("Price change from previous year: " + priceChange);
                }

                // Aktualizujemy zmienną previousYearPrice do średniej ceny z bieżącego roku
                previousYearPrice = pastPrice;
                count++;
            }
        }

        // Obliczamy średnie historyczne obłożenie
        double averageOccupancyRate = totalOccupancyRate / count;
        LOGGER.info("Average historical occupancy rate: " + averageOccupancyRate + "%");

        // Obliczamy średnią roczną zmianę ceny
        double averagePriceChange = totalPriceChange / (count - 1);
        LOGGER.info("Average yearly price change: " + averagePriceChange);

        // Predykcja ceny na przyszłość na podstawie średniej rocznej zmiany ceny
        double predictedPrice = previousYearPrice;
        for (int year = startDate.getYear(); year <= endDate.getYear(); year++) {
            predictedPrice *= (1 + averagePriceChange);
        }

        // Dostosowanie predykowanej ceny na podstawie oczekiwanego wskaźnika obłożenia
        double occupancyAdjustment = 1 - ((expectedOccupancyRate - averageOccupancyRate) / averageOccupancyRate);
        occupancyAdjustment = Math.max(0.1, occupancyAdjustment);  // Zapewnienie, że korekta nie spada poniżej 0.1

        // Finalna predykcja ceny
        double finalPrice = predictedPrice * occupancyAdjustment;

        LOGGER.info("Predicted price after occupancy adjustment: " + finalPrice);

        return finalPrice;
    }
}
