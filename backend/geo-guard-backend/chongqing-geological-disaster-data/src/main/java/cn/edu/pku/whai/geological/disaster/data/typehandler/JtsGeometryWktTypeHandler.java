/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Converts PostGIS geometry values to WKT strings through JTS.
 */
@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class JtsGeometryWktTypeHandler extends BaseTypeHandler<String> {

    private static final WKTWriter WKT_WRITER = new WKTWriter();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ps.setObject(i, parameter, Types.OTHER);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toWkt(rs.getObject(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toWkt(rs.getObject(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toWkt(cs.getObject(columnIndex));
    }

    private String toWkt(Object value) throws SQLException {
        if (value == null) {
            return null;
        }
        if (value instanceof Geometry geometry) {
            return WKT_WRITER.write(geometry);
        }
        if (value instanceof byte[] bytes) {
            return readWkb(bytes);
        }
        return readGeometry(value.toString());
    }

    private String readGeometry(String value) throws SQLException {
        if (value == null || value.isBlank()) {
            return value;
        }
        String normalized = value.trim();
        try {
            if (looksLikeHexWkb(normalized)) {
                return readWkb(WKBReader.hexToBytes(normalized));
            }
            return WKT_WRITER.write(new WKTReader().read(normalized));
        } catch (Exception e) {
            throw new SQLException("PostGIS geometry转WKT失败", e);
        }
    }

    private String readWkb(byte[] bytes) throws SQLException {
        try {
            return WKT_WRITER.write(new WKBReader().read(bytes));
        } catch (Exception e) {
            throw new SQLException("PostGIS geometry WKB转WKT失败", e);
        }
    }

    private boolean looksLikeHexWkb(String value) {
        return value.length() > 1 && value.length() % 2 == 0 && value.matches("[0-9A-Fa-f]+")
            && (value.startsWith("00") || value.startsWith("01"));
    }
}
