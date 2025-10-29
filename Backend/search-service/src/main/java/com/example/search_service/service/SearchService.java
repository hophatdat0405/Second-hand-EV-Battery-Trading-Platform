package com.example.search_service.service;

import com.example.search_service.client.ListingClient;
import com.example.search_service.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class SearchService {

    @Autowired
    private ListingClient listingClient;

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private static final Map<String,String> CONDITION_VALUE_TO_LABEL = new HashMap<>();
    static {
        // BỘ GIÁ TRỊ THỐNG NHẤT
        CONDITION_VALUE_TO_LABEL.put("99-100", "Mới 99% (Lướt)");
        CONDITION_VALUE_TO_LABEL.put("85-98", "Tốt 85-98% (Đã sử dụng)");
        CONDITION_VALUE_TO_LABEL.put("70-84", "Khá 70-84% (Cần bảo dưỡng)");
        CONDITION_VALUE_TO_LABEL.put("0-69", "Trung bình dưới 70%");
    }
    @Value("${product.service.url:http://localhost:8080}")
    private String listingServiceBaseUrl;

    /**
     * Normalize a string: remove diacritics, lower-case and trim.
     * Example: "Hà Nội" -> "ha noi"
     */
    private String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        return n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase().trim();
    }

    public List<SearchResultDTO> search(
            String type,
            String q,
            String location,
            String brand,
            String batteryType,
            Integer yearOfManufacture,
            Long priceMin,
            Long priceMax,
            String batteryCapacity,
            String mileageRange,
            String conditionName
    ) {
        // 1) Lấy tất cả listings từ listing-service qua Feign
        List<ProductListingDTO> listings;
        try {
            listings = listingClient.getAllListings();
            if (listings == null) listings = Collections.emptyList();
        } catch (Exception ex) {
            ex.printStackTrace();
            return Collections.emptyList();
        }

        // 2) Xây predicate theo params
        List<Predicate<ProductListingDTO>> predicates = new ArrayList<>();

        // type (keep original strict match, case-insensitive)
        if (StringUtils.hasText(type) && !"all".equalsIgnoreCase(type)) {
    final String requestedRaw = type.trim();
    final String requested = normalize(requestedRaw);

    // alias map: nhiều biến thể -> canonical key
    final Map<String, String> TYPE_ALIAS = new HashMap<>();
    TYPE_ALIAS.put("xe may", "motorcycle");
    TYPE_ALIAS.put("xe-may", "motorcycle");
    TYPE_ALIAS.put("xemay", "motorcycle");
    TYPE_ALIAS.put("xe may dien", "motorcycle");
    TYPE_ALIAS.put("xe-máy-điện", "motorcycle");
    TYPE_ALIAS.put("xe máy", "motorcycle");
    TYPE_ALIAS.put("motorbike", "motorcycle");
    TYPE_ALIAS.put("motorcycle", "motorcycle");

    TYPE_ALIAS.put("xe dap", "bike");
    TYPE_ALIAS.put("xe-dap", "bike");
    TYPE_ALIAS.put("xedap", "bike");
    TYPE_ALIAS.put("xe dap dien", "bike");
    TYPE_ALIAS.put("bike", "bike");
    TYPE_ALIAS.put("bicycle", "bike");

    TYPE_ALIAS.put("oto", "car");
    TYPE_ALIAS.put("o to", "car");
    TYPE_ALIAS.put("ô tô", "car");
    TYPE_ALIAS.put("car", "car");

    TYPE_ALIAS.put("pin", "battery");
    TYPE_ALIAS.put("battery", "battery");

    // map requested -> canonical (fallback: requested itself)
    final String mappedRequested = TYPE_ALIAS.getOrDefault(requested, requested);

    // add predicate: normalize productType and compare flexibly
    predicates.add(pl -> {
        ProductDTO p = pl.getProduct();
        if (p == null || p.getProductType() == null) return false;
        String ptRaw = p.getProductType();
        String pt = normalize(ptRaw);

        // 1) direct canonical equality
        String ptMapped = TYPE_ALIAS.getOrDefault(pt, pt);

        if (ptMapped.equals(mappedRequested)) return true;

        // 2) contains checks (handle cases like "xe may dien - electric motorcycle" etc.)
        if (pt.contains(mappedRequested) || mappedRequested.contains(pt)) return true;

        // 3) sometimes productType stores descriptive phrase containing english/vietnamese words
        // check if normalized productType contains key words used in requestedRaw
        if (!requestedRaw.equals(mappedRequested)) {
            String reqToken = normalize(requestedRaw).replaceAll("[^a-z0-9\\s-]", "").trim();
            if (!reqToken.isEmpty() && (pt.contains(reqToken) || reqToken.contains(pt))) return true;
        }

        return false;
    });
}

        // q (tìm kiếm chính) -> dùng normalize + contains
        if (StringUtils.hasText(q)) {
            String qq = normalize(q);
            predicates.add(pl -> {
                ProductDTO p = pl.getProduct();
                if (p == null) return false;
                String name = normalize(p.getProductName());
                String desc = normalize(p.getDescription());
                return (name.contains(qq) || desc.contains(qq));
            });
        }

        // location -> contains + normalize (thay equals)
        if (StringUtils.hasText(location)) {
            String loc = normalize(location);
            predicates.add(pl -> pl.getLocation() != null && normalize(pl.getLocation()).contains(loc));
        }

        // brand -> contains + normalize
        if (StringUtils.hasText(brand)) {
            String br = normalize(brand);
            predicates.add(pl -> {
                ProductSpecificationDTO s = pl.getProduct() != null ? pl.getProduct().getSpecification() : null;
                return s != null && normalize(s.getBrand()).contains(br);
            });
        }

        // batteryType -> contains + normalize
        if (StringUtils.hasText(batteryType)) {
            String bt = normalize(batteryType);
            predicates.add(pl -> {
                ProductSpecificationDTO s = pl.getProduct() != null ? pl.getProduct().getSpecification() : null;
                return s != null && normalize(s.getBatteryType()).contains(bt);
            });
        }

        // yearOfManufacture exact match
        if (yearOfManufacture != null) {
            predicates.add(pl -> {
                ProductSpecificationDTO s = pl.getProduct() != null ? pl.getProduct().getSpecification() : null;
                return s != null && Objects.equals(s.getYearOfManufacture(), yearOfManufacture);
            });
        }

        // price min/max
        if (priceMin != null) {
            predicates.add(pl -> {
                ProductDTO p = pl.getProduct();
                return p != null && p.getPrice() != null && p.getPrice() >= priceMin;
            });
        }
        if (priceMax != null) {
            predicates.add(pl -> {
                ProductDTO p = pl.getProduct();
                return p != null && p.getPrice() != null && p.getPrice() <= priceMax;
            });
        }

        // batteryCapacity -> contains + normalize
        if (StringUtils.hasText(batteryCapacity)) {
            String bc = normalize(batteryCapacity);
            predicates.add(pl -> {
                ProductSpecificationDTO s = pl.getProduct() != null ? pl.getProduct().getSpecification() : null;
                return s != null && normalize(s.getBatteryCapacity()).contains(bc);
            });
        }

        // conditionName -> contains + normalize
        if (StringUtils.hasText(conditionName)) {
        String rawCond = conditionName.trim();
        String cnNormalized = normalize(rawCond);

        // 1. Phân tích giá trị range từ frontend (vd: "0-69", "70-84")
        Integer parsedMin = null;
        Integer parsedMax = null;
        boolean isRange = rawCond.contains("-");
        
        if (isRange) {
            try {
                String[] parts = rawCond.split("-");
                String a = parts.length > 0 ? parts[0].replaceAll("[^0-9]", "") : "";
                String b = parts.length > 1 ? parts[1].replaceAll("[^0-9]", "") : "";
                if (!a.isEmpty()) parsedMin = Integer.valueOf(a);
                if (!b.isEmpty()) parsedMax = Integer.valueOf(b);
                if (parsedMin != null && parsedMax == null && parsedMin < 100) parsedMax = 100;
            } catch (Exception ex) {
                log.warn("Lỗi parse numeric range cho conditionName: {}", rawCond);
            }
        }

        final Integer finalMin = parsedMin;
        final Integer finalMax = parsedMax;
        final String finalMappedLabel = CONDITION_VALUE_TO_LABEL.get(rawCond.replaceAll("\\s+", ""));

        predicates.add(pl -> {
            ProductSpecificationDTO s = pl.getProduct() != null ? pl.getProduct().getSpecification() : null;
            if (s == null || s.getCondition() == null || s.getCondition().getConditionName() == null) {
                return false;
            }
            String itemCondRaw = s.getCondition().getConditionName(); 
            String itemCondName = normalize(itemCondRaw);

            // A) Nếu giá trị là range (vd: "0-69"), ưu tiên kiểm tra bằng numeric range
            if (isRange && finalMin != null && finalMax != null) {
                // Trích xuất phần trăm từ itemCondRaw (vd: "Tốt 87% (Đã sử dụng)" -> 87)
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,3})\\s*%?").matcher(itemCondRaw);
                if (m.find()) {
                    try {
                        int val = Integer.parseInt(m.group(1));
                        if (val >= finalMin && val <= finalMax) return true;
                    } catch (Exception ex) {
                        // Bỏ qua lỗi parse số
                    }
                }
                
                // DỰ PHÒNG TEXT MATCH
                if (finalMappedLabel != null && itemCondName.contains(normalize(finalMappedLabel))) return true;
            }

            // B) Khớp bằng text/nhãn: Dùng cho mọi trường hợp
            if (itemCondName.contains(cnNormalized)) return true;
            
            return false;
        });
      }
        //Quãng đường
        Long mileageMin = null, mileageMax = null;
        if (StringUtils.hasText(mileageRange)) {
            String mr = mileageRange.trim();
            try {
                if (mr.contains("-")) {
                    String[] parts = mr.split("-");
                    mileageMin = Long.parseLong(parts[0].replaceAll("[^0-9]", ""));
                    mileageMax = Long.parseLong(parts[1].replaceAll("[^0-9]", ""));
                } else if (mr.startsWith("<")) {
                    mileageMax = Long.parseLong(mr.replaceAll("[^0-9]", ""));
                } else if (mr.startsWith(">")) {
                    mileageMin = Long.parseLong(mr.replaceAll("[^0-9]", ""));
                }
            } catch (Exception ex) {
                // ignore parse error
            }
            if (mileageMin != null || mileageMax != null) {
                Long finalMin = mileageMin;
                Long finalMax = mileageMax;
                predicates.add(pl -> {
                    ProductSpecificationDTO s = pl.getProduct() != null ? pl.getProduct().getSpecification() : null;
                    if (s == null || s.getMileage() == null) return false;
                    long val = s.getMileage();
                    if (finalMin != null && finalMax != null) return val >= finalMin && val <= finalMax;
                    if (finalMin != null) return val >= finalMin;
                    return finalMax != null && val <= finalMax;
                });
            }
        }

        // 3) Áp dụng predicates để filter
        List<ProductListingDTO> filtered = listings.stream()
                .filter(pl -> {
                    for (Predicate<ProductListingDTO> pred : predicates) {
                        if (!pred.test(pl)) return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(ProductListingDTO::getListingDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        // 4) Map sang SearchResultDTO
        List<SearchResultDTO> results = filtered.stream().map(pl -> {
            ProductDTO p = pl.getProduct();
            ProductSpecificationDTO s = p != null ? p.getSpecification() : null;
            SearchResultDTO dto = new SearchResultDTO();
            if (p != null) {
                dto.setProductId(p.getProductId());
                dto.setProductName(p.getProductName());
                dto.setProductType(p.getProductType());
                dto.setPrice(p.getPrice());

                if (p.getImages() != null && !p.getImages().isEmpty()) {
                    String base = (listingServiceBaseUrl != null) ? listingServiceBaseUrl.replaceAll("/$", "") : "";
                    List<String> urls = p.getImages().stream()
                    .map(ProductImageDTO::getImageUrl)
                    .filter(Objects::nonNull)
                    .map(u -> {
                        String trimmed = u.trim();
                        if (trimmed.isEmpty()) return null;
                        // absolute url -> giữ nguyên
                        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed;
                        // protocol-relative -> thêm http:
                        if (trimmed.startsWith("//")) return "http:" + trimmed;
                        // đường dẫn tương đối bắt đầu bằng /
                        if (trimmed.startsWith("/")) {
                            if (!base.isEmpty()) return base + trimmed;
                            return trimmed;
                        }
                        // chỉ tên file hoặc relative path không bắt đầu bằng /
                        if (!base.isEmpty()) return base + "/" + trimmed;
                        return trimmed;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                    if (!urls.isEmpty()) dto.setImageUrls(urls);
                }
        }

            dto.setLocation(pl.getLocation());
            if (s != null) {
                dto.setYearOfManufacture(s.getYearOfManufacture());
                dto.setBrand(s.getBrand());
                dto.setBatteryCapacity(s.getBatteryCapacity());
                dto.setMileage(s.getMileage());
                dto.setBatteryType(s.getBatteryType());
                if (s.getCondition() != null) dto.setConditionName(s.getCondition().getConditionName());
            }
            return dto;
        }).collect(Collectors.toList());

        return results;
    }
}
