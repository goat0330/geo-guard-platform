/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 将 JSON 中的时间字段反序列化为毫秒时间戳：支持数字、数字字符串、以及常见日期时间字符串。
 */
public class MillisOrDateStringDeserializer extends JsonDeserializer<Long> {

    private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    };

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public Long deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.currentToken();
        if (t == null) {
            return null;
        }
        if (t.isNumeric()) {
            return p.getLongValue();
        }
        if (t == JsonToken.VALUE_STRING) {
            String s = p.getText();
            if (s == null) {
                return null;
            }
            s = s.trim();
            if (s.isEmpty()) {
                return null;
            }
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException ignored) {
            }
            for (DateTimeFormatter f : DATE_TIME_FORMATTERS) {
                try {
                    LocalDateTime ldt = LocalDateTime.parse(s, f);
                    return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                } catch (DateTimeParseException ignored) {
                }
            }
            try {
                LocalDate d = LocalDate.parse(s, DATE_FORMAT);
                return d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } catch (DateTimeParseException e) {
                return (Long) ctxt.handleWeirdStringValue(Long.class, s, "非有效时间戳或日期时间");
            }
        }
        if (t == JsonToken.VALUE_NULL) {
            return null;
        }
        return (Long) ctxt.handleUnexpectedToken(Long.class, p);
    }
}
