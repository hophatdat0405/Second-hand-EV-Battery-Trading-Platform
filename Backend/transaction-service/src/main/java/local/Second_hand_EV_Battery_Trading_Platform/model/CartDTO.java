package local.Second_hand_EV_Battery_Trading_Platform.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDTO {
    private String productName; // Tên sản phẩm
    private double price;       // Giá sản phẩm
}
