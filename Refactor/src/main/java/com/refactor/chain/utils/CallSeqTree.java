package com.refactor.chain.utils;



import com.refactor.chain.analyzer.layer.LayerType;
import com.refactor.chain.utils.toJson.Point;
import com.refactor.chain.utils.toJson.PointTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallSeqTree {
//    private final Node root; // 整颗树的根结点，无语义

    // 用于快速查找已存在的节点
    private final Map<String, Node> nodeMap = new HashMap<>();


    public CallSeqTree() {
    }

    /**
     * 获取所有节点
     *
     * @return 节点列表
     */
    public List<Node> getNodes() {
        return new ArrayList<>(nodeMap.values());
    }


    /**
     * 获取一个节点
     *
     * @param id   节点名称，通常为类名
     * @return 节点对象
     */
    public Node getNode(String id) {
        Node result = nodeMap.get(id);
        if (result == null) {
            System.out.println("Node " + id + " 不存在");
            return null;
        } else {
            return result;
        }
    }

    /**
     * 添加一个节点
     *
     * @param id    节点唯一标识
     * @param type  节点类型
     */
    public void addNode(String id, LayerType type) {
        if (!nodeMap.containsKey(id)) {
            Node newNode = new Node(id, type);
            nodeMap.put(id, newNode);
        } else {
            System.out.println("Node " + id + " 已存在");
        }
    }

    /**
     * 添加边从源节点到目标节点
     *
     * @param sourceId       源节点ID
     * @param targetId       目标节点ID
     * @param sourceType     源节点类型（用于判断调用层次）
     */
    public void addEdge(String sourceId, String targetId, LayerType sourceType) {
        LayerType targetType = determineTargetLayer(sourceType);
        if (targetType == null) {
            System.out.println("从 " + sourceId + " 到 " + targetId + " 不符合调用关系");
            return; // 不符合从Controller到Service等的调用关系
        }

        Node sourceNode = getNode(sourceId);
        Node targetNode = getNode(targetId);

        for (Edge edge : sourceNode.getEdges()) {
            if (edge.getTarget().equals(targetNode)) {
                return; // 避免重复添加边
            }
        }

        sourceNode.addEdge(targetNode);
    }

    /**
     * 根据源节点类型确定目标节点类型
     *
     * @param sourceType 源节点类型
     * @return 目标节点类型
     */
    private LayerType determineTargetLayer(LayerType sourceType) {
        switch (sourceType) {
            case CONTROLLER:
                return LayerType.SERVICE;
            case SERVICE:
                return LayerType.REPOSITORY;
            case REPOSITORY:
                return LayerType.ENTITY;
            default:
                return null;
        }
    }

    public PointTree convertToPointTree() {
        // 将callSeqTree中所有指向service的边改为指向serviceImpl的边
        List<Node> nodes = getNodes();
        for (Node node : nodes) {
            List<Edge> edges = node.getEdges();
            System.out.println("NODE " +node.getId());
            for (Edge edge : edges) {
                if (edge.getTarget().getId().endsWith("Service:SERVICE")) {
                    System.out.println("------------------"
                    );
                    String[] split = edge.getTarget().getId().split(":");
                    System.out.println("split[0]" +split[0]);
                    String target = split[0].substring(split[0].lastIndexOf(".")+1,split[0].length());
                    String target2 = split[0].substring(0, split[0].lastIndexOf(".")) +".impl." + target + "Impl:SERVICE";
                    for (Node node1 : nodes) {
                        System.out.println(node1.getId());
                        if (node1.getId().contains("."+target + "Impl:SERVICE")) {
                            System.out.println("++++++++++++");
                            edge.setTarget(node1);
                        } else if (node1.getId().endsWith(target) && !node1.getId().equals(target)) {
                            System.out.println("++++++++++++------");
                            edge.setTarget(node1);

                        }
                    }
                }
            }
        }
        for (Node node: nodes)
            System.out.println(node.getId());

        Point root = new Point("root");
        PointTree pointTree = new PointTree(root);
        for (Node node : getNodes()) {
            Point point = new Point(node.getId());
            if (node.getType() == LayerType.CONTROLLER) {
                root.addChild(point);
                buildCallSeq(node, point);
            }
        }
        return pointTree;
    }

    private void buildCallSeq(Node node, Point point) {
        for (Edge edge : node.getEdges()) {
            Node target = edge.getTarget();
            LayerType originType = node.getType();
            LayerType targetType = target.getType();
            if (!isValidLayerTransition(originType, targetType)) {
                continue;
            }
            Point child = new Point(target.getId());
            point.addChild(child);
            buildCallSeq(target, child);
        }
    }

    private boolean isValidLayerTransition(LayerType callerLayer, LayerType calleeLayer) {
        // 定义层之间有效的调用关系
        switch (callerLayer) {
            case CONTROLLER:
                return calleeLayer == LayerType.SERVICE;
            case SERVICE:
                return calleeLayer == LayerType.REPOSITORY || calleeLayer == LayerType.MAPPER;
            case REPOSITORY:
                return calleeLayer == LayerType.ENTITY;
            default:
                return false;
        }
    }
}