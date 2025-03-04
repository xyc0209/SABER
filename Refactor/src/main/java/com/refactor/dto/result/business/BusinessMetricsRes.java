package com.refactor.dto.result.business;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BusinessMetricsRes {

    /**
     * date format: yyyy-MM-dd HH:mm:ss
     */
    @Schema(description = "末点时刻", example = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    @Schema(description = "起点时刻", example = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    /**
     * interval equals to startTime - endTime
     * unit: min
     */
    @Schema(description = "时间间隔 单位：分")
    private Integer interval;
    /**
     * unique id: language-serviceName-podName
     */
    @Schema(description = "微服务所用语言")
    private String language;
    @Schema(description = "微服务名称")
    private String serviceName;
    /**
     * 需要设置 ELASTIC_APM_SERVICE_NODE_NAME 保证所有数据所属服务实例的唯一性 不产生混淆
     * 因为可能存在同一服务器部署同一服务的多个实例，造成数据混乱
     */
    @Schema(description = "微服务实例POD标识")
    private String podName;
    @Schema(description = "业务层方法的调用统计，键为方法名称，值为调用次数",example = "public final java.lang.Class com.yang.xingdiancloud.service.impl.UserUnitRelationService$$SpringCGLIB$$0.getTargetClass():1")
    private Map<String, Long> businessCallMap;

}
