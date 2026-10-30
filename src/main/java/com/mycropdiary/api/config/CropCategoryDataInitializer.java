package com.mycropdiary.api.config;

import com.mycropdiary.api.entity.cultivation.CropCategory;
import com.mycropdiary.api.repository.cultivation.CropCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tự động khởi tạo danh mục các loại cây trồng ngắn ngày (short-term crops) chuẩn VietGAP vào cơ sở dữ liệu.
 */
@Component
public class CropCategoryDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CropCategoryDataInitializer.class);
    private final CropCategoryRepository cropCategoryRepository;

    public CropCategoryDataInitializer(CropCategoryRepository cropCategoryRepository) {
        this.cropCategoryRepository = cropCategoryRepository;
    }

    private record SeedCrop(String name, String desc, String imageUrl) {}

    @Override
    @Transactional
    public void run(String... args) {
        List<SeedCrop> shortTermCrops = List.of(
            new SeedCrop("Rau Muống", "Cây rau ngắn ngày ăn lá, chu kỳ thu hoạch 25-30 ngày", "https://images.unsplash.com/photo-1540420773420-3366772f4999"),
            new SeedCrop("Xà Lách Mỡ (Lettuce)", "Cây rau ăn lá ngắn ngày trồng thủy canh hoặc đất sạch, chu kỳ 35-45 ngày", "https://images.unsplash.com/photo-1556801712-76c8eb07bbc9"),
            new SeedCrop("Cải Thìa (Bok Choy)", "Rau ăn lá ngắn ngày giàu dinh dưỡng VietGAP, chu kỳ 30-40 ngày", "https://images.unsplash.com/photo-1594282486552-05b4d80fbb9f"),
            new SeedCrop("Cải Bó Xôi (Spinach)", "Cây rau ngắn ngày ôn đới giàu sắt và khoáng chất, chu kỳ 35-45 ngày", "https://images.unsplash.com/photo-1576045057995-568f588f82fb"),
            new SeedCrop("Cải Ngọt", "Cây rau ngắn ngày phổ biến, sinh trưởng khỏe, chu kỳ 25-35 ngày", "https://images.unsplash.com/photo-1540420773420-3366772f4999"),
            new SeedCrop("Dưa Leo (Dưa Chuột)", "Cây rau ăn quả ngắn ngày giàn leo năng suất cao, chu kỳ 45-60 ngày", "https://images.unsplash.com/photo-1604977042946-1eecc30f269e"),
            new SeedCrop("Đậu Cô Ve (Đậu Que)", "Cây họ đậu ngắn ngày cố định đạm sinh học, chu kỳ 50-60 ngày", "https://images.unsplash.com/photo-1551462147-37885acc36f1"),
            new SeedCrop("Đậu Bắp", "Cây ăn quả ngắn ngày chịu nhiệt tốt, giàu chất xơ, chu kỳ 45-55 ngày", "https://images.unsplash.com/photo-1425543103986-22abb7d7e8d2"),
            new SeedCrop("Cà Rốt Baby", "Cây ăn củ ngắn ngày VietGAP giòn ngọt, chu kỳ 60-75 ngày", "https://images.unsplash.com/photo-1598170845058-32b9d6a5da37"),
            new SeedCrop("Củ Cải Trắng", "Cây ăn củ ngắn ngày sinh trưởng nhanh, chu kỳ 40-50 ngày", "https://images.unsplash.com/photo-1590779033100-9f60a05a013d"),
            new SeedCrop("Hành Lá & Rau Thơm", "Cây gia vị ngắn ngày thu hoạch liên tục quanh năm, chu kỳ 30-40 ngày", "https://images.unsplash.com/photo-1615485290382-441e4d049cb5"),
            new SeedCrop("Rau Mồng Tơi", "Cây rau ăn lá ngắn ngày thanh nhiệt mùa hè, chu kỳ 30-40 ngày", "https://images.unsplash.com/photo-1540420773420-3366772f4999"),
            new SeedCrop("Bắp Cải", "Cây rau cuốn bắp ngắn ngày vụ đông xuân, chu kỳ 65-75 ngày", "https://images.unsplash.com/photo-1550828520-4cb496926fc9"),
            new SeedCrop("Dưa Hấu", "Cây ăn quả ngắn ngày ruột đỏ ngọt mát, chu kỳ 60-70 ngày", "https://images.unsplash.com/photo-1587049352846-4a222e784d38"),
            new SeedCrop("Bí Đao Chanh", "Cây ăn quả ngắn ngày leo giàn VietGAP, chu kỳ 60-70 ngày", "https://images.unsplash.com/photo-1570715621210-b74704bdfa57"),
            new SeedCrop("Bắp Ngọt (Ngô)", "Cây lương thực ngắn ngày chất lượng cao, chu kỳ 70-80 ngày", "https://images.unsplash.com/photo-1551754655-cd27e38d2076")
        );

        for (SeedCrop crop : shortTermCrops) {
            if (cropCategoryRepository.findByCategoryNameIgnoreCase(crop.name).isEmpty()) {
                CropCategory entity = new CropCategory(crop.name, crop.desc);
                entity.setImageUrl(crop.imageUrl);
                cropCategoryRepository.save(entity);
                log.info("Initialized short-term crop category: {}", crop.name);
            }
        }
    }
}

