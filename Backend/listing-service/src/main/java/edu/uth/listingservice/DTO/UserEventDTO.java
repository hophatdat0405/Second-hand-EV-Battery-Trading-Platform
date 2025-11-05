// File: src/main/java/edu/uth/listingservice/DTO/UserEventDTO.java
package edu.uth.listingservice.DTO;

/**
 * DTO này chứa thông tin người dùng được gửi qua RabbitMQ từ User Service
 * để đồng bộ hóa.
 */
public class UserEventDTO {
    private Integer id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String eventType; // Ví dụ: "USER_CREATED", "USER_UPDATED"

    public UserEventDTO() {}

    // Constructor (tùy chọn)
    // Cần phải có Getters và Setters để Jackson có thể hoạt động

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
}