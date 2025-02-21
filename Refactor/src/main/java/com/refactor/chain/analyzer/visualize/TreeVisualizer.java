package com.refactor.chain.analyzer.visualize;


import com.refactor.chain.utils.CallSeqTree;
import com.refactor.chain.utils.Edge;
import com.refactor.chain.utils.Node;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;


public class TreeVisualizer {

    /**
     * 可视化调用顺序树
     *
     * @param callSeqTree CallSeqTree 实例，包含所有节点和边的信息
     */
    public void visualize(CallSeqTree callSeqTree) {


        // 设置 GraphStream 使用 Swing 界面
        System.setProperty("org.graphstream.ui", "swing");

        // 创建一个新的图
        Graph graph = new SingleGraph("Call Sequence Graph");

        // 为每个用户定义的节点在 GraphStream 图中添加一个节点
        for (Node node : callSeqTree.getNodes()) {
            String nodeId = node.getId();

            // 检查 GraphStream 图中是否已经存在该节点
            if (graph.getNode(nodeId) == null) {
                org.graphstream.graph.Node graphNode = graph.addNode(nodeId);
                graphNode.addAttribute("ui.label", nodeId);
                String color = getColorByLayer(node.getType().toString());
                graphNode.addAttribute("ui.style", "fill-color: " + color + ";");
            }
        }

        // 为每个用户定义的节点的每条边在 GraphStream 图中添加一条边
        int edgeCounter = 0; // 用于生成唯一的边 ID
        for (Node node : callSeqTree.getNodes()) {
            String sourceId = node.getId();
            for (Edge edge : node.getEdges()) {
                String targetId = edge.getTarget().getId();
                String edgeId = "E" + edgeCounter++;

                // 检查 GraphStream 图中是否已经存在这条边
                // GraphStream 不允许多条相同 ID 的边存在
                // 如果存在，可以选择增加权重或忽略
                try {
                    // 尝试添加边，如果边已存在会抛出异常
                    org.graphstream.graph.Edge graphEdge = graph.addEdge(edgeId, sourceId, targetId, true);
                    graphEdge.addAttribute("ui.label", edge.getWeight());
                } catch (Exception e) {
                    // 边已经存在，可以选择累加权重或记录日志
                    System.out.println("边已存在: " + sourceId + " -> " + targetId);
                }

            }
        }

        // 设置图的总体样式
        graph.setAttribute("ui.stylesheet",
                "node { size: 20px; text-size: 14px; }" +
                        "edge { text-size: 10px; arrow-size: 10px; }"
        );

        // 显示图
        graph.display();
    }

    /**
     * 根据层类型返回对应的颜色，用于节点的可视化
     *
     * @param layerType 节点的层类型
     * @return 对应的颜色字符串
     */
    private String getColorByLayer(String layerType) {
        switch (layerType) {
            case "CONTROLLER":
                return "red";
            case "SERVICE":
                return "green";
            case "REPOSITORY":
                return "blue";
            case "ENTITY":
                return "yellow";
            case "ROOT":
                return "black";
            default:
                return "grey";
        }
    }
}
