package com.refactor.chain.utils.echart;

import static spark.Spark.get;
import static spark.Spark.port;

public class EChartsServer {

    public static void startServer(String jsonString) {
        // 启动服务器并监听 4567 端口
        port(4567);

        // 路由到首页，返回包含 ECharts 图表的 HTML 页面
        get("/", (req, res) -> {
            res.type("text/html");
            return "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                    "    <title>ECharts Tree Example</title>\n" +
                    "    <style>\n" +
                    "        #main {\n" +
                    "            width: 100%;\n" +
                    "            height: 600px;\n" +
                    "        }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "\n" +
                    "<div id=\"main\"></div>\n" +
                    "\n" +
                    "<!-- 使用 type=\"module\" 加载 ECharts 模块 -->\n" +
                    "<script type=\"module\">\n" +
                    "    // 从 ECharts 模块导入\n" +
                    "    import * as echarts from 'https://cdn.jsdelivr.net/npm/echarts@5.4.2/dist/echarts.esm.min.js';\n" +
                    "\n" +
                    "    // 获取图表DOM元素\n" +
                    "    var chartDom = document.getElementById('main');\n" +
                    "    var myChart = echarts.init(chartDom);\n" +
                    "    var option;\n" +
                    "\n" +
                    "    // 加载数据\n" +
                    "    myChart.showLoading();\n" +
                    "    var data = " + jsonString + ";\n" +
                    "    myChart.hideLoading();\n" +
                    "\n" +
                    "    // 设置初始状态\n" +
                    "    data.children.forEach(function (datum, index) {\n" +
                    "        index % 2 === 0 && (datum.collapsed = true);\n" +
                    "    });\n" +
                    "\n" +
                    "    // 配置项\n" +
                    "    myChart.setOption(\n" +
                    "        (option = {\n" +
                    "            tooltip: {\n" +
                    "                trigger: 'item',\n" +
                    "                triggerOn: 'mousemove'\n" +
                    "            },\n" +
                    "            series: [\n" +
                    "                {\n" +
                    "                    type: 'tree',\n" +
                    "                    data: [data],\n" +
                    "                    top: '1%',\n" +
                    "                    left: '7%',\n" +
                    "                    bottom: '1%',\n" +
                    "                    right: '20%',\n" +
                    "                    symbolSize: 7,\n" +
                    "                    label: {\n" +
                    "                        position: 'left',\n" +
                    "                        verticalAlign: 'middle',\n" +
                    "                        align: 'right',\n" +
                    "                        fontSize: 9\n" +
                    "                    },\n" +
                    "                    leaves: {\n" +
                    "                        label: {\n" +
                    "                            position: 'right',\n" +
                    "                            verticalAlign: 'middle',\n" +
                    "                            align: 'left'\n" +
                    "                        }\n" +
                    "                    },\n" +
                    "                    emphasis: {\n" +
                    "                        focus: 'descendant'\n" +
                    "                    },\n" +
                    "                    expandAndCollapse: true,\n" +
                    "                    animationDuration: 550,\n" +
                    "                    animationDurationUpdate: 750\n" +
                    "                }\n" +
                    "            ]\n" +
                    "        })\n" +
                    "    );\n" +
                    "\n" +
                    "    option && myChart.setOption(option);\n" +
                    "</script>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html>";
        });

        System.out.println("Server started at: http://localhost:4567");
    }
}
