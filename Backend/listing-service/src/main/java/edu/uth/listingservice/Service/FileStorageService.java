package edu.uth.listingservice.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path rootLocation;

    // Cấu hình thư mục lưu trữ ảnh
    public FileStorageService() {
        // Ảnh sẽ được lưu vào thư mục 'uploads' trong project của bạn
        this.rootLocation = Paths.get("uploads"); 
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location", e);
        }
    }

    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Failed to store empty file.");
        }

        // Tạo một tên file ngẫu nhiên để tránh trùng lặp
        String extension = getFileExtension(file.getOriginalFilename());
        String newFileName = UUID.randomUUID().toString() + "." + extension;

        try {
            Path destinationFile = this.rootLocation.resolve(Paths.get(newFileName))
                    .normalize().toAbsolutePath();

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            // Trả về đường dẫn công khai để truy cập file
            return "/uploads/" + newFileName;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    public void delete(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        try {
            // Lấy tên file từ đường dẫn URL (ví dụ: /uploads/abc.jpg -> abc.jpg)
            String filename = filePath.substring(filePath.lastIndexOf("/") + 1);
            Path file = rootLocation.resolve(filename);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // Log lỗi thay vì throw exception để không làm dừng chương trình
            System.err.println("Could not delete file: " + filePath + ". Error: " + e.getMessage());
        }
    }
}