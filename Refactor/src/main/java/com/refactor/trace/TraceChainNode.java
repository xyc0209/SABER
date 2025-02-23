package com.refactor.trace;

import com.refactor.dto.result.sql.DBModel;
import com.refactor.utils.TraceUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.*;

/**
 * @description:
 * @author: xyc
 * @date: 2024-11-28 14:21
 */
@Data
public class TraceChainNode {
    @Schema(description = "当前服务实例名称",example = "Java|cloud-user-service|win-20230627bhi")
    private String serviceName;
    @Schema(description = "当前请求的接口名称",example = "UserController#getDataById")
    private String APIName;
    /**
     * 当前请求是否包含关系型数据库查询
     */
    @Schema(description = "当前请求是否包含关系型数据库查询[MYSQL,oracle,SQLSERVER,POSTGRESQL,MariaDB,SQLITE,SYBASE]")

    private Boolean containSQL;
    /**
     * 若是containSQL为false 此属性为null
     * 每一条数据库请求记录为一个DBModel
     */
    @Schema(description = "若是containSQL为false 此属性为null,否则每一条数据库请求记录为一个DBModel")
    private List<DBModel> sqlModelList;
    @Schema(description = "当前请求的子请求")
    private List<TraceChainNode> subNodes;
    private int count = 0;
    private int countPR = 0;

    public TraceChainNode() {
        subNodes = new ArrayList<>();
    }

    public void addSubNode(TraceChainNode traceChainNode) {
        subNodes.add(traceChainNode);
    }


    public int getTargetRelationCounts(String parentServiceName, String childServiceName){
    // 检查 subNodes 是否为 null 或空
        if (subNodes != null && !subNodes.isEmpty()) {
        // 遍历子节点
        for (TraceChainNode subNode : subNodes) {
            // 检查父节点是否匹配
            if (this.serviceName.equals(parentServiceName) && subNode.getServiceName().equals(childServiceName)) {
                count++;
            }
            // 递归遍历子节点
            count += subNode.getTargetRelationCounts(parentServiceName, childServiceName);
        }
    }
        return count;

}
   //
   // 递归遍历, 回溯
   public  void collectServiceNames(int serviceCount,Set<String> serviceNames, Map<List<String>, Integer> chainMap) {
       if (this.serviceName == null) {
           return;
       }
       // 添加当前节点的 serviceName 到 Set 中
       if (this.serviceName != null) {
           serviceNames.add(this.serviceName);
           serviceCount++;
       }

       // 遍历子节点
       if (!this.subNodes.isEmpty()) {
           for (TraceChainNode subNode : this.subNodes) {
               subNode.collectServiceNames(serviceCount,serviceNames, chainMap);
           }
       }
       else {
           chainMap.put(new ArrayList<>(serviceNames), serviceCount);
       }

       // 回退时移除当前节点的 serviceName
       if (this.serviceName != null) {
           serviceNames.remove(this.serviceName);
       }
   }

    public int getCountsPR(Set<String> serviceSet, Map<String, Map<String, Integer>> callNumMapPR){
        // 检查 subNodes 是否为 null 或空
        if (subNodes != null && !subNodes.isEmpty()) {
            callNumMapPR.put(this.serviceName, new HashMap<>());
            // 遍历子节点
            countPR +=subNodes.size();
            Map<String, Integer> callMap = new HashMap<>();
            for (TraceChainNode subNode : subNodes) {
                String serviceName = subNode.getServiceName();
                serviceSet.add(serviceName);
                callMap.put(serviceName, callMap.getOrDefault(serviceName, 0) + 1);
            }
            for (String service: callNumMapPR.keySet()){
                for (String sameService: callMap.keySet()){
                    Integer Count = callNumMapPR.get(service).get(sameService);
                    Integer newCount = callMap.get(sameService);
                    if (callNumMapPR.get(service).containsKey(sameService)){
                        if(newCount > Count)
                            callNumMapPR.get(service).put(sameService, newCount);
                    }
                    else{
                        callNumMapPR.get(service).put(sameService, newCount);
                    }
                }

            }
            for (TraceChainNode subNode : subNodes) {
                String serviceName = subNode.getServiceName();
                serviceSet.add(serviceName);
                countPR += subNode.getCountsPR(serviceSet, callNumMapPR);
            }
        }
        return countPR;

    }


}