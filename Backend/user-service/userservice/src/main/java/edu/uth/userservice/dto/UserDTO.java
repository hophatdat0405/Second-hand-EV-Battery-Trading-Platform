package edu.uth.userservice.dto;

import edu.uth.userservice.model.Role;
import edu.uth.userservice.model.User;

import java.util.Set;
import java.util.stream.Collectors;

public class UserDTO {
    private Integer userId;
    private String name;
    private String email;
    private String phone;
    private String address;
    private Set<String> roles;

    public UserDTO() {}

    public UserDTO(User u) {
        this.userId = u.getUserId();
        this.name = u.getName();
        this.email = u.getEmail();
        this.phone = u.getPhone();
        this.address = u.getAddress();
        if (u.getRoles() != null) {
            this.roles = u.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        }
    }

    // getters / setters...
    public Integer getUserId(){ return userId; }
    public String getName(){ return name; }
    public String getEmail(){ return email; }
    public String getPhone(){ return phone; }
    public String getAddress(){ return address; }
    public Set<String> getRoles(){ return roles; }

    public void setUserId(Integer userId){ this.userId = userId; }
    public void setName(String name){ this.name = name; }
    public void setEmail(String email){ this.email = email; }
    public void setPhone(String phone){ this.phone = phone; }
    public void setAddress(String address){ this.address = address; }
    public void setRoles(Set<String> roles){ this.roles = roles; }
}
