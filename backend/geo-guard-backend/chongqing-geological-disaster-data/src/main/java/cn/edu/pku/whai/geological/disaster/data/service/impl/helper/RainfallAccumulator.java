/* @author kongweiguang */
package cn.edu.pku.whai.geological.disaster.data.service.impl.helper;

/**
 * 雨量累加器（内部类提取）。
 */
public final class RainfallAccumulator {
    public double total;
    public int count;

    public void add(Double rainfall) {
        total += rainfall == null ? 0D : rainfall;
        count++;
    }

    public double average() {
        return count == 0 ? 0D : total / count;
    }
}
