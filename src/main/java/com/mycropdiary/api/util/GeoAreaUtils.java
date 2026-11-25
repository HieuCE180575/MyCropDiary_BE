package com.mycropdiary.api.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Tiện ích tính toán và chuẩn hóa diện tích (m²) từ tọa độ đa giác bản đồ và đơn vị đo (m2 / ha).
 */
public final class GeoAreaUtils {

    private static final double EARTH_RADIUS_M = 6378137.0;
    private static final BigDecimal HA_TO_M2 = BigDecimal.valueOf(10000);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private GeoAreaUtils() {
    }

    /**
     * Chuyển đổi giá trị diện tích theo đơn vị ('m2', 'm²', 'ha') sang m².
     */
    public static BigDecimal toSquareMeters(BigDecimal value, String unit) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (unit != null && "ha".equalsIgnoreCase(unit.trim())) {
            return value.multiply(HA_TO_M2);
        }
        return value;
    }

    /**
     * Tính diện tích đa giác (m²) từ chuỗi JSON ranh giới boundary_coordinates.
     * Hỗ trợ định dạng [{"lat": ..., "lng": ...}] hoặc [[lat, lng], ...].
     * Trả về 0 nếu không đủ 3 đỉnh hợp lệ hoặc diện tích bằng 0.
     */
    public static double calculatePolygonAreaM2(String boundaryCoordinates) {
        if (boundaryCoordinates == null || boundaryCoordinates.isBlank()) {
            return 0.0;
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(boundaryCoordinates);
            if (!root.isArray() || root.size() < 3) {
                return 0.0;
            }
            List<double[]> points = new ArrayList<>();
            for (JsonNode item : root) {
                Double lat = null;
                Double lng = null;
                if (item.isArray() && item.size() >= 2 && item.get(0).isNumber() && item.get(1).isNumber()) {
                    lat = item.get(0).asDouble();
                    lng = item.get(1).asDouble();
                } else if (item.isObject() && item.has("lat") && item.has("lng")
                        && item.get("lat").isNumber() && item.get("lng").isNumber()) {
                    lat = item.get("lat").asDouble();
                    lng = item.get("lng").asDouble();
                }
                if (lat != null && lng != null && Double.isFinite(lat) && Double.isFinite(lng)) {
                    points.add(new double[]{lat, lng});
                }
            }
            if (points.size() < 3) {
                return 0.0;
            }
            double lat0Rad = Math.toRadians(points.get(0)[0]);
            double refLat = points.get(0)[0];
            double refLng = points.get(0)[1];
            double area = 0.0;
            for (int i = 0; i < points.size(); i++) {
                double[] p1 = points.get(i);
                double[] p2 = points.get((i + 1) % points.size());
                double x1 = Math.toRadians(p1[1] - refLng) * EARTH_RADIUS_M * Math.cos(lat0Rad);
                double y1 = Math.toRadians(p1[0] - refLat) * EARTH_RADIUS_M;
                double x2 = Math.toRadians(p2[1] - refLng) * EARTH_RADIUS_M * Math.cos(lat0Rad);
                double y2 = Math.toRadians(p2[0] - refLat) * EARTH_RADIUS_M;
                area += (x1 * y2) - (x2 * y1);
            }
            return Math.round(Math.abs(area) / 2.0);
        } catch (Exception ex) {
            return 0.0;
        }
    }
}
