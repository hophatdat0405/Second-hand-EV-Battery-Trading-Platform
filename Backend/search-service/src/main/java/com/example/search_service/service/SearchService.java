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
        CONDITION_VALUE_TO_LABEL.put("99-100", "Mới 99% (Lướt)");
        CONDITION_VALUE_TO_LABEL.put("85-98", "Tốt 85-98% (Đã sử dụng)");
        CONDITION_VALUE_TO_LABEL.put("70-84", "Khá 70-84% (Cần bảo dưỡng)");
        CONDITION_VALUE_TO_LABEL.put("0-69", "Trung bình dưới 70%");
    }
    @Value("${product.service.url:http://localhost:8080}")
    private String listingServiceBaseUrl;

    private String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        return n.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase().trim();
    }

    // map requested type (many variants) -> canonical type (car, motorbike, bike, battery)
    private String mapRequestedTypeToCanonical(String requestedType) {
        if (requestedType == null) return null;
        String r = requestedType.trim().toLowerCase();
        r = r.replaceAll("[\\s_]+", " ");

        Map<String, String> MAP = new HashMap<>();
        // car
        MAP.put("oto", "car"); MAP.put("o to", "car"); MAP.put("ô tô", "car"); MAP.put("o-to", "car");
        MAP.put("car", "car"); MAP.put("ôto", "car"); MAP.put("xe ôtô", "car"); MAP.put("xe oto", "car");
        // motorbike
        MAP.put("xemay", "motorbike"); MAP.put("xe may", "motorbike"); MAP.put("xe máy", "motorbike");
        MAP.put("xe-may", "motorbike"); MAP.put("motorbike", "motorbike"); MAP.put("motorcycle", "motorbike");
        MAP.put("motor", "motorbike"); MAP.put("xe máy điện", "motorbike");
        // bike
        MAP.put("xedap", "bike"); MAP.put("xe dap", "bike"); MAP.put("xe đạp", "bike");
        MAP.put("bike", "bike"); MAP.put("bicycle", "bike");
        // battery
        MAP.put("pin", "battery"); MAP.put("battery", "battery");

        if (MAP.containsKey(r)) return MAP.get(r);

        // fuzzy: remove non-alphanum and compare
        String normalized = r.replaceAll("[^a-z0-9]", "");
        for (Map.Entry<String, String> e : MAP.entrySet()) {
            String keynorm = e.getKey().replaceAll("[^a-z0-9]", "");
            if (keynorm.equals(normalized)) return e.getValue();
        }

        // contains fallback
        if (r.contains("oto") || r.contains("ô tô") || r.contains("car")) return "car";
        if (r.contains("xemay") || r.contains("xe máy") || r.contains("motor")) return "motorbike";
        if (r.contains("xedap") || r.contains("xe đạp") || r.contains("bike")) return "bike";
        if (r.contains("pin") || r.contains("battery")) return "battery";

        return null;
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
        // map type -> canonical
        final String canonicalType = mapRequestedTypeToCanonical(type);

        // 1) Try to call listing-service with canonicalType so listing-service can filter server-side
        List<ProductListingDTO> listings = Collections.emptyList();
        boolean usedServerSideTypeFilter = false;
        try {
            if (canonicalType != null) {
                // try server-side filtered call
                listings = listingClient.getAllListings(canonicalType, "date", 500);
                if (listings == null) listings = Collections.emptyList();
                usedServerSideTypeFilter = true;
            } else {
                // no canonical type -> fetch all
                listings = listingClient.getAllListings(null, "date", 500);
                if (listings == null) listings = Collections.emptyList();
            }
        } catch (Exception ex) {
            log.warn("listingClient.getAllListings(...) failed with canonicalType={}, will fallback to permissive call. Error: {}",
                    canonicalType, ex.toString());
            // fallback: try calling parameterless method if available
            try {
                // if your ListingClient still has single method without params, uncomment the next line:
                // listings = listingClient.getAllListings();
                // Otherwise leave listings empty
                listings = Collections.emptyList();
            } catch (Exception ex2) {
                log.error("Fallback listing fetch failed: {}", ex2.toString());
                listings = Collections.emptyList();
            }
        }

        // 2) Build predicates for other filters.
        List<Predicate<ProductListingDTO>> predicates = new ArrayList<>();

        // If listing-service didn't apply type filter (canonicalType == null OR server call failed),
        // then add type predicate on server-side (client-side) to keep behavior robust.
        if (!usedServerSideTypeFilter && StringUtils.hasText(type) && !"all".equalsIgnoreCase(type)) {
            final String requestedRaw = type.trim();
            final String mappedRequested = mapRequestedTypeToCanonical(requestedRaw);
            final String mappedRequestedSafe = mappedRequested != null ? mappedRequested : normalize(requestedRaw);

            predicates.add(pl -> {
                ProductDTO p = pl.getProduct();
                if (p == null) return false;
                String ptRaw = p.getProductType();
                String pt = normalize(ptRaw);

                // map productType to canonical quickly
                String ptCanon = mapRequestedTypeToCanonical(ptRaw);
                if (ptCanon != null && ptCanon.equals(mappedRequestedSafe)) return true;

                // contains checks on productType/name/brand
                if (pt.contains(mappedRequestedSafe) || mappedRequestedSafe.contains(pt)) return true;

                ProductSpecificationDTO s = p.getSpecification();
                String specBrand = s != null && s.getBrand() != null ? normalize(s.getBrand()) : "";
                String name = p.getProductName() != null ? normalize(p.getProductName()) : "";
                if (!specBrand.isEmpty() && (specBrand.contains(mappedRequestedSafe) || mappedRequestedSafe.contains(specBrand))) return true;
                if (!name.isEmpty() && (name.contains(mappedRequestedSafe) || mappedRequestedSafe.contains(name))) return true;

                // final keyword fallback
                if ("car".equals(mappedRequestedSafe) && (pt.contains("car") || pt.contains("oto") || name.contains("vinfast"))) return true;
                if ("motorbike".equals(mappedRequestedSafe) && (pt.contains("motor") || pt.contains("xemay") || name.contains("xemay"))) return true;
                if ("bike".equals(mappedRequestedSafe) && (pt.contains("bike") || pt.contains("xedap") || name.contains("xedap"))) return true;
                if ("battery".equals(mappedRequestedSafe) && (pt.contains("battery") || pt.contains("pin") || name.contains("pin"))) return true;

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

        // conditionName -> contains + normalize (existing logic)
        if (StringUtils.hasText(conditionName)) {
            String rawCond = conditionName.trim();
            String cnNormalized = normalize(rawCond);

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

                if (isRange && finalMin != null && finalMax != null) {
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,3})\\s*%?").matcher(itemCondRaw);
                    if (m.find()) {
                        try {
                            int val = Integer.parseInt(m.group(1));
                            if (val >= finalMin && val <= finalMax) return true;
                        } catch (Exception ex) { }
                    }
                    if (finalMappedLabel != null && itemCondName.contains(normalize(finalMappedLabel))) return true;
                }

                if (itemCondName.contains(cnNormalized)) return true;
                return false;
            });
        }

        // mileage parsing (same as before)
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
            } catch (Exception ex) { }
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

        // 3) apply predicates
        List<ProductListingDTO> filtered = listings.stream()
                .filter(pl -> {
                    for (Predicate<ProductListingDTO> pred : predicates) {
                        if (!pred.test(pl)) return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(ProductListingDTO::getListingDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        // 4) map to SearchResultDTO
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
                                if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed;
                                if (trimmed.startsWith("//")) return "http:" + trimmed;
                                if (trimmed.startsWith("/")) {
                                    if (!base.isEmpty()) return base + trimmed;
                                    return trimmed;
                                }
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
