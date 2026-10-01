/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl.helper;

import java.util.Objects;

/**
 * 网格位置（内部类提取）。
 */
public final class GridLocation {
    public final Double lon;
    public final Double lat;

    public GridLocation(Double lon, Double lat) {
        this.lon = lon;
        this.lat = lat;
    }

    public Double lon() { return lon; }
    public Double lat() { return lat; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof GridLocation other)) {
            return false;
        }
        return Objects.equals(lon, other.lon) && Objects.equals(lat, other.lat);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lon, lat);
    }
}
