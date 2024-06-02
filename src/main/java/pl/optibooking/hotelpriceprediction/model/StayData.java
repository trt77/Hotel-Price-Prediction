package pl.optibooking.hotelpriceprediction.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;

@Entity
public class StayData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDate beginOfStay;
    private LocalDate endOfStay;
    private int persons;
    private String roomType;
    private double totalPrice;
    private int occupiedRooms;  // Add this field
    private int totalRooms;     // Add this field

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getBeginOfStay() {
        return beginOfStay;
    }

    public void setBeginOfStay(LocalDate beginOfStay) {
        this.beginOfStay = beginOfStay;
    }

    public LocalDate getEndOfStay() {
        return endOfStay;
    }

    public void setEndOfStay(LocalDate endOfStay) {
        this.endOfStay = endOfStay;
    }

    public int getPersons() {
        return persons;
    }

    public void setPersons(int persons) {
        this.persons = persons;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public int getOccupiedRooms() {
        return occupiedRooms;
    }

    public void setOccupiedRooms(int occupiedRooms) {
        this.occupiedRooms = occupiedRooms;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(int totalRooms) {
        this.totalRooms = totalRooms;
    }
}
