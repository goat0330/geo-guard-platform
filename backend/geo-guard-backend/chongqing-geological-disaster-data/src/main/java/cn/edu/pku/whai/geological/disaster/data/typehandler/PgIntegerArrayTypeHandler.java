/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * PostgreSQL int4[] 与 Integer 列表之间的类型转换处理器
 */
public class PgIntegerArrayTypeHandler extends BaseTypeHandler<List<Integer>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Integer> parameter, JdbcType jdbcType) throws SQLException {
        Array sqlArray = ps.getConnection().createArrayOf("integer", parameter.toArray(new Integer[0]));
        ps.setArray(i, sqlArray);
    }

    @Override
    public List<Integer> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toIntegerList(rs.getArray(columnName));
    }

    @Override
    public List<Integer> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toIntegerList(rs.getArray(columnIndex));
    }

    @Override
    public List<Integer> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toIntegerList(cs.getArray(columnIndex));
    }

    private List<Integer> toIntegerList(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return null;
        }
        try {
            Object array = sqlArray.getArray();
            if (!(array instanceof Object[] values)) {
                return List.of();
            }
            return Arrays.stream(values)
                .map(value -> value == null ? null : ((Number) value).intValue())
                .toList();
        } finally {
            sqlArray.free();
        }
    }
}
