package edu.uth.listingservice.DTO;

import lombok.Data;
import java.util.List;
import java.util.Set; // <--  THÊM IMPORT NÀY

@Data
public class UserDTO {
    private Integer id; // <-- SỬA TỪ Long thành Integer
    private String name;
    private String email;
    private String phone;
    private String address;
    private Set<String> roles; // <--  SỬA TỪ List thành Set
}