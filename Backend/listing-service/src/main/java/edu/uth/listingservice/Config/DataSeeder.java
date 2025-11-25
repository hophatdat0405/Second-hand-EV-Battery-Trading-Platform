package edu.uth.listingservice.Config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import edu.uth.listingservice.Model.ProductCondition;
import edu.uth.listingservice.Repository.ProductConditionRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private ProductConditionRepository conditionRepository;

    @Override
    public void run(String... args) throws Exception {
        // Kiểm tra xem bảng đã có dữ liệu chưa
        if (conditionRepository.count() == 0) {
            System.out.println("⚡ [DataSeeder] Bảng product_conditions đang trống. Đang khởi tạo dữ liệu...");

            // Tạo các đối tượng (Không cần set ID nếu Database là Auto Increment, 
            // nó sẽ tự sinh ra 1, 2, 3, 4 theo thứ tự insert)
            
            ProductCondition c1 = new ProductCondition();
            // Giả sử model của bạn có setter setConditionName, hoặc constructor
            c1.setConditionName("Mới 99% (Lướt)");

            ProductCondition c2 = new ProductCondition();
            c2.setConditionName("Tốt 85-94% (Đã sử dụng)");

            ProductCondition c3 = new ProductCondition();
            c3.setConditionName("Khá 70-84% (Cần bảo dưỡng)");

            ProductCondition c4 = new ProductCondition();
            c4.setConditionName("Trung bình dưới 70%");

            // Lưu vào DB
            List<ProductCondition> conditions = Arrays.asList(c1, c2, c3, c4);
            conditionRepository.saveAll(conditions);

            System.out.println("✅ [DataSeeder] Đã insert thành công 4 loại tình trạng sản phẩm.");
        } else {
            System.out.println("✅ [DataSeeder] Dữ liệu product_conditions đã tồn tại. Bỏ qua khởi tạo.");
        }
    }
}