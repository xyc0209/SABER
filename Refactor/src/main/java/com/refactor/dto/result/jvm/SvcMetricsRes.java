package com.refactor.dto.result.jvm;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SvcMetricsRes {

    /**
     * date format: yyyy-MM-dd HH:mm:ss
     */
    @Schema(description = "末点时刻",example = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    @Schema(description = "起点时刻",example = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    /**
     * interval equals to startTime - endTime
     */
    @Schema(description = "时间间隔 单位：分")
    private Integer interval;
    /**
     * unique id: language-serviceName-podName
     */
    @Schema(description = "编程语言")
    private String language;
    @Schema(description = "服务名")
    private String serviceName;
    /**
     * 需要设置 ELASTIC_APM_SERVICE_NODE_NAME 保证所有数据所属服务实例的唯一性 不产生混淆
     * 因为可能存在同一服务器部署同一服务的多个实例，造成数据混乱
     */
    @Schema(description = "当前实例的名称")
    private String podName;

    //先统计heap三部分内存池的内存信息eden survivor old


    private Map<String,List<JVMMemoryRes>> memoryRes;

    private Map<String, List<JVMGCRes>> gcRes;

//    @Schema(description = "伊甸园区内存使用情况")
//    private List<JVMMemoryRes> edenPool;
//    @Schema(description = "幸存者区内存使用情况")
//    private List<JVMMemoryRes> survivorPool;
//    @Schema(description = "老年代内存使用情况")
//    private List<JVMMemoryRes> oldPool;
//    @Schema(description = "元数据区域内存使用情况")
//    private List<JVMMemoryRes> metaSpacePool;
//    @Schema(description = "codeCache区域内存使用情况")
//    private List<JVMMemoryRes> codeCachePool;
//    @Schema(description = "compressed class space内存使用情况")
//    private List<JVMMemoryRes> compressedClaSacPool;
//    @Schema(description = "JVM垃圾回收情况")
//    private List<JVMGCRes> jvmGCInYoung;
//    @Schema(description = "JVM老年代垃圾回收情况")
//    private List<JVMGCRes> jvmGCInOld;

    @Schema(description = "JVM相关数据总结列表")
    private List<JVMSummaryRes> jvmSummaryList;



//    @Override
//    public String toString() {
//        return "SvcMetricsRes{" +
//                "startTime=" + startTime + "\n" +
//                ", endTime=" + endTime + "\n" +
//                ", interval=" + interval + "\n" +
//                ", language='" + language + '\'' + "\n" +
//                ", serviceName='" + serviceName + '\'' + "\n" +
//                ", podName='" + podName + '\'' + "\n" +
//                ", edenPool=" + edenPool + "\n" +
//                ", survivorPool=" + survivorPool + "\n" +
//                ", oldPool=" + oldPool + "\n" +
//                ", metaSpacePool=" + metaSpacePool + "\n" +
//                ", codeCachePool=" + codeCachePool + "\n" +
//                ", compressedClaSacPool=" + compressedClaSacPool + "\n" +
//                ", jvmGCInYoung=" + jvmGCInYoung + "\n" +
//                ", jvmGCInOld=" + jvmGCInOld + "\n" +
//                ", jvmSummaryList=" + jvmSummaryList + "\n" +
//                '}';
//    }
}
