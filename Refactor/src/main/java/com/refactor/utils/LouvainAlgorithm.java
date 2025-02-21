package com.refactor.utils;


import com.refactor.chain.analyzer.layer.LayerType;
import com.refactor.chain.utils.Node;
import com.refactor.chain.utils.Edge;

import java.util.*;
import java.util.stream.Collectors;

public class LouvainAlgorithm {
    private List<Node> nodes;

    public LouvainAlgorithm(List<Node> nodes) {
        this.nodes = nodes;
    }


    private static void countNodeOccurrences(Node node, Set<Node> visited, Map<Node, Long> nodeCount) {
        if (node == null || visited.contains(node)) {
            return; // 如果节点为空或者已访问，则返回
        }

        visited.add(node); // 标记当前节点为已访问
        nodeCount.put(node, nodeCount.getOrDefault(node, 0L) + 1); // 增加出现次数

        // 遍历当前节点的边
        for (Edge edge : node.getEdges()) {
            countNodeOccurrences(edge.getTarget(), visited, nodeCount); // 递归遍历目标节点
        }
    }

    // 使用深度优先搜索来找到所有直接连接和间接连接的节点
    private static void dfs(Node node, Community community) {
        if (node == null) {
            return; // 如果节点为空，则返回
        }


        if (!community.getMembers().contains(node))
            community.getMembers().add(node); // 将节点加入到当前社区中
        else
            return;
        // 遍历当前节点的所有边
        for (Edge edge : node.getEdges()) {
            Node targetNode = edge.getTarget();
            // 直接进行深度优先搜索，遍历所有连接的节点，无需设置访问标志
            dfs(targetNode, community); // 递归遍历与当前节点相连的目标节点
        }
    }

    // 使用Louvain算法检测社区
    public List<Community> detectCommunities() {
        List<Community> communities = new ArrayList<>();

        // 遍历所有节点，进行DFS
        for (Node node : nodes) {
            // 每次遇到一个新的节点，都创建一个新的社区并执行DFS
            Community community = new Community();
            dfs(node, community); // 不使用visited集合，遍历所有可能的连接
            communities.add(community);
        }

        // 在这里过滤掉无效的社区（如果需要的话）
        List<Community> finalCommunities = new ArrayList<>();
        for (Community community : communities) {
            if (community.isValid(false)) { // 检查社区是否有效，具体的规则可以根据需求修改
                finalCommunities.add(community);
            }
        }
        System.out.println("communities SIZE"+finalCommunities.size());
        for (Community community: finalCommunities)
            System.out.println("+++++++ : " +community.toString());
        // 返回最终的社区列表
        return mergeCommunities(finalCommunities);
    }
    public static List<Community> mergeCommunities(List<Community> communities) {
        List<Community> mergedCommunities = new ArrayList<>();
        Set<Node> visited = new HashSet<>();

        for (Community community : communities) {
            // 如果该社区的成员已经被访问过，则跳过
            if (visited.containsAll(community.members)) {
                continue;
            }

            Community newCommunity = new Community();
            newCommunity.members = new HashSet<>();
            Set<Node> currentMembers = new HashSet<>(community.members);
            Queue<Set<Node>> queue = new LinkedList<>();
            queue.add(currentMembers);

            while (!queue.isEmpty()) {
                Set<Node> currentSet = queue.poll();
                // 添加当前社区的成员到新社区
                newCommunity.members.addAll(currentSet);
                visited.addAll(currentSet);

                // 查找与当前成员有公共元素的其他社区
                for (Community otherCommunity : communities) {
                    if (!visited.containsAll(otherCommunity.members) && hasCommonNodes(currentSet, otherCommunity.members)) {
                        queue.add(new HashSet<>(otherCommunity.members));
                    }
                }
            }

            mergedCommunities.add(newCommunity);
        }

        return mergedCommunities;
    }

    // 检查两个集合是否有公共元素
    private static boolean hasCommonNodes(Set<Node> set1, Set<Node> set2) {
        for (Node node : set1) {
            if (set2.contains(node)) {
                return true;
            }
        }
        return false;
    }
//    public List<Community> detectCommunities() {
//        List<Community> communities = new ArrayList<>();
//        Map<Node, Long> nodeCount = new HashMap<>();
//
//        // 遍历每个 Node，递归记录每个 Node 的出现次数
//        for (Node node : nodes) {
//            // 使用一个 Set 追踪已访问的节点，以避免循环
//            Set<Node> visited = new HashSet<>();
//            countNodeOccurrences(node, visited, nodeCount);
//        }
//
//        List<Node> filteredNodes = nodes.stream()
//                .filter(node -> !(nodeCount.get(node) == 1 && node.getType() == LayerType.OTHER)) // 删除条件
//                .collect(Collectors.toList());
//
//        // 更新原始列表
//        nodes.clear();
//        nodes.addAll(filteredNodes);
//
//
//        System.out.println("        nodes.addAll(filteredNodes);" +nodes.toString());
//        boolean ifExistSvcLayer = false;
//        for (Node node : nodes) {
//            if (node.getType() == LayerType.SERVICE)
//                ifExistSvcLayer = true;
//            Community community = new Community();
//            community.members.add(node);
//            System.out.println(node.getId());
//            communities.add(community);
//        }
//
//        //删除与其他node无任何关联的community
//
//        System.out.println(communities.toString());
//        boolean improvement = true; // 初始化为 true
//        while (improvement) {
//            System.out.println("=====================================");
//            improvement = false; // 每次循环开始时重置为 false
//            for (Node node : nodes) {
//                System.out.println("node" +node.toString());
//                Community currentCommunity = findCommunity(node, communities);
//                double bestModularityGain = 0;
//                Community bestCommunity = currentCommunity;
//
//                boolean allIndependent = true;
//                for (int i = 0; i < communities.size(); i++) {
//                    for (int j = i + 1; j < communities.size(); j++) {
//                        if (hasConnection(communities.get(i), communities.get(j))) {
//                            allIndependent = false;
//                            break;
//                        }
//                    }
//                    if (!allIndependent) break;
//                }
////                System.out.println("allIndependent"+allIndependent);
//                // 如果所有社区都独立，则退出循环
//                if (allIndependent) {
//                    break; // 所有社区独立，退出循环
//                }
//
//                for (Community community : communities) {
//                    if (community != currentCommunity && community.getAllEdges().contains(node)) {
//                        double gain = calculateModularityGain(node, community, currentCommunity);
//                        System.out.println("GAIN: " + gain);
//                        if (gain > bestModularityGain) {
//                            bestModularityGain = gain;
//                            bestCommunity = community;
//                        }
//                    }
//                }
//                System.out.println("bestCommunity" + bestCommunity);
//                System.out.println("currentCommunity" + currentCommunity.toString());
//                System.out.println("======");
//                System.out.println("bestCommunity" + bestCommunity);
//                System.out.println("currentCommunity" + currentCommunity);
//                System.out.println("bestModularityGain" + bestModularityGain);
//                if (bestCommunity.equals(currentCommunity))
//                    break;
//                if (bestCommunity != currentCommunity && bestModularityGain > 0) {
//                    currentCommunity.members.remove(node);
//                    bestCommunity.members.add(node);
//                    improvement = true; // 找到增益，设置为 true
//                }
//            }
//
//            // 后处理，确保每个社区有效并合并无效社区
//            List<Community> invalidCommunities = new ArrayList<>();
////            System.out.println("communities11111111111" + communities.toString());
//            for (Community community : communities) {
//                if (!community.isValid(ifExistSvcLayer)) {
//                    invalidCommunities.add(community);
//                }
//            }
////            System.out.println(invalidCommunities.isEmpty());
//            // 如果存在有效社区
//            if (!invalidCommunities.isEmpty()) {
////                System.out.println("invalidCommunities------------" + invalidCommunities.toString());
//                boolean finalIfExistSvcLayer = ifExistSvcLayer;
//                Community validCommunity = communities.stream()
//                        .filter(community -> community.isValid(finalIfExistSvcLayer))
//                        .findFirst()
//                        .orElse(null);
//
//                if (validCommunity != null) {
//                    // 将无效社区与有效社区进行合并
//                    List<Community> toRemove = new ArrayList<>();
//                    for (Community invalidCommunity : invalidCommunities) {
//                        System.out.println("validCommunity" + validCommunity);
//                        System.out.println("invalidCommunity" + invalidCommunity);
//                        System.out.println(hasConnection(validCommunity, invalidCommunity));
//                        if (hasConnection(validCommunity, invalidCommunity)) {
//                            System.out.println("validCommunity" + validCommunity);
//                            System.out.println("invalidCommunity" + invalidCommunity);
//                            validCommunity.members.addAll(invalidCommunity.members);
//                            toRemove.add(invalidCommunity);
//                        }
//
//
//                    }
//                    // 移除合并的无效社区
//                    communities.removeAll(toRemove);
//                    invalidCommunities.removeAll(toRemove);
//                }
//                while (!invalidCommunities.isEmpty()) {
//                    System.out.println("--------"+invalidCommunities.toString());
//                    Community firstInvalid = invalidCommunities.remove(0);
//                    Community mergedCommunity = new Community();
//                    mergedCommunity.members.addAll(firstInvalid.members);
//
//                    // 合并所有相连的无效社区
//                    List<Community> toRemove = new ArrayList<>();
//                    boolean isMerged = false;
//                    System.out.println("invalidCommunities" +invalidCommunities.toString());
//                    for (Community invalidCommunity : invalidCommunities) {
//                        if (hasConnection(mergedCommunity, invalidCommunity)) {
//                            isMerged = true;
//                            mergedCommunity.members.addAll(invalidCommunity.members);
//                            toRemove.add(invalidCommunity);
//                            communities.remove(invalidCommunity);
//                        }
//                    }
//                    System.out.println("mergedCommunity"+mergedCommunity.toString());
//                    System.out.println("toRemove"+toRemove.toString());
//                    // 移除已合并的无效社区
//                    invalidCommunities.removeAll(toRemove);
//                    communities.remove(firstInvalid); // 移除原始的无效社区
//                    communities.remove(toRemove);
//                    communities.add(mergedCommunity); // 添加合并后的社区
//                    System.out.println("communities"+communities.toString());
//                    // 检查合并后的社区是否有效
//                    if (mergedCommunity.isValid(ifExistSvcLayer)) {
//                        improvement = true; // 需要再次检查增益
//                        break; // 如果形成有效社区，跳出循环
//                    }
//                    System.out.println("tttttttttttttttttttttttttttttttt");
//                }
//
//                System.out.println("invalidCommunities::"+invalidCommunities.toString());
//                //删除只有一个member的无效社区，孤独的无效节点
//                // 处理完无效社区后，重新计算增益
//                improvement = true; // 需要再次检查增益
////                System.out.println("invalidCommunities" + invalidCommunities.toString());
//            }
//            if (invalidCommunities.isEmpty())
//                break;
//
//            System.out.println("communitities:  "+communities.toString());
//        }
////        communities.removeIf(community -> community.getMembers().size() < 4);
//        List<Community> finalCommunities = new ArrayList<>();
//        for (Community community: communities){
//            if (community.isValid(false))
//                finalCommunities.add(community);
//        }
//        System.out.println("finalCommunities:  "+finalCommunities.toString());
//        System.out.println("finalCommunities size" +finalCommunities.size());
//        return finalCommunities;
//    }

    // 检查两个社区之间是否存在连接
    private boolean hasConnection(Community community1, Community community2) {
        for (Node node1 : community1.members) {
            for (Edge edge : node1.getEdges()) {
                if (community2.members.contains(edge.getTarget())) {
                    return true; // 找到连接
                }
            }
        }
        for (Node node2 : community2.members) {
            for (Edge edge : node2.getEdges()) {
                if (community1.members.contains(edge.getTarget())) {
                    return true; // 找到连接
                }
            }
        }
        return false; // 没有连接
    }

    private Community findCommunity(Node node, List<Community> communities) {
        for (Community community : communities) {
            if (community.members.contains(node)) {
                return community;
            }
        }
        return null;
    }

    private double calculateModularityGain(Node node, Community newCommunity, Community oldCommunity) {
        double totalEdges = nodes.stream().flatMap(n -> n.getEdges().stream()).count(); // 计算总边数
        double nodeDegree = node.getEdges().size(); // 当前节点的度

        // 计算新社区中当前节点的度
        double degreeInNewCommunity = newCommunity.members.stream()
                .flatMap(n -> n.getEdges().stream())
                .filter(edge -> newCommunity.members.contains(edge.getTarget()))
                .count();

        // 计算新社区中所有节点的总度
        double newCommunityDegree = newCommunity.members.stream().mapToDouble(n -> n.getEdges().size()).sum();

        // 计算模块化增益
        double gain = (degreeInNewCommunity / 2 * totalEdges)
                - (newCommunityDegree * nodeDegree) / 2 * (totalEdges * totalEdges);

        return gain;
    }



    public static void main(String[] args) {
        List<Node> nodes = new ArrayList<>();

//        Node controller1 = new Node("Controller1", "Controller");
//        Node controller3 = new Node("Controller3", "Controller");
//        Node serviceImpl1 = new Node("ServiceImpl1", "ServiceImpl");
//        Node repository1 = new Node("Repository1", "Repository");
//        Node entity1 = new Node("Entity1", "Entity");
//
//        Node controller2 = new Node("Controller2", "Controller");
//        Node serviceImpl2 = new Node("ServiceImpl2", "ServiceImpl");
//        Node repository2 = new Node("Repository2", "Repository");
//        Node entity2 = new Node("Entity2", "Entity");


//        Node controller3 = new Node("Controller3", "Controller");
//        Node serviceImpl3 = new Node("ServiceImpl3", "ServiceImpl");

//        controller1.addEdge(serviceImpl1);
//        controller3.addEdge(serviceImpl1);
//        serviceImpl1.addEdge(repository1);
//        repository1.addEdge(entity1);
//
//        controller2.addEdge(serviceImpl2);
//        serviceImpl2.addEdge(repository2);
//        repository2.addEdge(entity2);
//
//        nodes.add(controller1);
//        nodes.add(controller3);
//        nodes.add(serviceImpl1);
//        nodes.add(repository1);
//        nodes.add(entity1);
//
//        nodes.add(controller2);
//        nodes.add(serviceImpl2);
//        nodes.add(repository2);
//        nodes.add(entity2);
        LouvainAlgorithm algorithm = new LouvainAlgorithm(nodes);
        List<Community> communities = algorithm.detectCommunities();

//        for (Community community : communities) {
//            System.out.println("Community: " + community.members.stream().map(n -> n.id).collect(Collectors.toList()));
//            for (Node node : community.members) {
//                if(node.getType().equals("Controller")) {
////                    node.dfsWithTwoOrMoreParents(node);
//                    for (Map.Entry<Node, Set<Node>> entry : node.dfsWithTwoOrMoreParents(node,community.getParentCountMap(),).entrySet()) {
//                        Node key = entry.getKey();
//                        if (entry.getValue().size()>=2)
//                            System.out.println("Key: " + key.id + ", Size: " + entry.getValue().size());
//                    }
//                }
//            }
//            System.out.println(""+community.getParentCountMap().size());
//        }

    }
    public static List<String> getNodeIds(Community community) {
        List<String> nodeIds = new ArrayList<>();

        Set<Node> members = community.getMembers();
        for (Node node : members) {
            String id = node.getId();
            int lastDotIndex = id.lastIndexOf('.');
            int colonIndex = id.lastIndexOf(':');
            if (lastDotIndex != -1 && colonIndex != -1 && lastDotIndex < colonIndex) {
                // 返回 '.' 和 ':' 之间的子串
                nodeIds.add(id.substring(lastDotIndex + 1, colonIndex));
            }
        }

        return nodeIds;
    }}