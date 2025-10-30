package edu.uth.userservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
// Thay "items" bằng tên bảng thật của bạn nếu khác
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // owner id (khớp user.user_id)
    @Column(name = "owner_id")
    private Integer ownerId;

    private String title;
    private String type; // "car" | "battery" ...
    private Long price;
    private Integer year;
    private Integer km;
    private String itemCondition;


    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Item() {}

    // getters / setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getOwnerId() { return ownerId; }
    public void setOwnerId(Integer ownerId) { this.ownerId = ownerId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getKm() { return km; }
    public void setKm(Integer km) { this.km = km; }
    public String getCondition() { return itemCondition; }
    public void setCondition(String condition) { this.itemCondition = condition; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
