package com.picture_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.picture_backend.model.entity.ApiMetric;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ApiMetricMapper extends BaseMapper<ApiMetric> {

    /** 小时桶累加（唯一键 bucketHour+uri+method） */
    @Insert("INSERT INTO api_metric (bucketHour, uri, method, requestCount, errorCount, totalCostMs, maxCostMs) "
            + "VALUES (#{bucketHour}, #{uri}, #{method}, #{requestCount}, #{errorCount}, #{totalCostMs}, #{maxCostMs}) "
            + "ON DUPLICATE KEY UPDATE requestCount = requestCount + VALUES(requestCount), "
            + "errorCount = errorCount + VALUES(errorCount), "
            + "totalCostMs = totalCostMs + VALUES(totalCostMs), "
            + "maxCostMs = GREATEST(maxCostMs, VALUES(maxCostMs))")
    int upsert(@Param("bucketHour") Date bucketHour, @Param("uri") String uri, @Param("method") String method,
               @Param("requestCount") long requestCount, @Param("errorCount") long errorCount,
               @Param("totalCostMs") long totalCostMs, @Param("maxCostMs") long maxCostMs);

    /** 按接口聚合（Top N，按调用量倒序） */
    @Select("SELECT uri, method, SUM(requestCount) AS requestCount, SUM(errorCount) AS errorCount, "
            + "SUM(totalCostMs) AS totalCostMs, MAX(maxCostMs) AS maxCostMs "
            + "FROM api_metric WHERE bucketHour >= #{from} "
            + "GROUP BY uri, method ORDER BY requestCount DESC LIMIT #{limit}")
    List<Map<String, Object>> summary(@Param("from") Date from, @Param("limit") int limit);

    /** 全窗口总计（不受 Top N 限制） */
    @Select("SELECT SUM(requestCount) AS requestCount, SUM(errorCount) AS errorCount, " 
            + "SUM(totalCostMs) AS totalCostMs, MAX(maxCostMs) AS maxCostMs " 
            + "FROM api_metric WHERE bucketHour >= #{from}")
    Map<String, Object> totals(@Param("from") Date from);

    /** 按小时聚合（趋势图） */
    @Select("SELECT bucketHour AS bucketHour, SUM(requestCount) AS requestCount, SUM(errorCount) AS errorCount, "
            + "SUM(totalCostMs) AS totalCostMs FROM api_metric WHERE bucketHour >= #{from} "
            + "GROUP BY bucketHour ORDER BY bucketHour")
    List<Map<String, Object>> hourly(@Param("from") Date from);
}
