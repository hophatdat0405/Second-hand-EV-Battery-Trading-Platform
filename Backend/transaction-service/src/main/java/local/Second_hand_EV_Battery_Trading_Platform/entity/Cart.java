package local.Second_hand_EV_Battery_Trading_Platform.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity @Table(name = "cart")
@Data @NoArgsConstructor @AllArgsConstructor
public class Cart {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long accountId;
  private String productName;
  private Double price;
  private Integer quantity;
  private String status; // UNPAID / PAID
  private LocalDateTime createdAt = LocalDateTime.now();

  // total là generated column trong DB nên không map trường này (tuỳ chọn):
  @Transient
  public Double getTotal() {
    if (price == null || quantity == null) return 0.0;
    return price * quantity;
  }
}