package org.etd.framework.starter.es.utils;

import java.util.*;

public class Test {

    public static void main(String[] args) {
        // 准备测试标题内容数据
        List<String> titleList = new ArrayList<>();
        titleList.add("有哪些养猫必须知道的冷知识");
        titleList.add("有哪些养猫必须知道的冷");
        titleList.add("有哪些养猫必须知道");
        titleList.add("有哪些养猫");
        titleList.add("有哪些");

        // 原始标题内容数据
        String originalTitle = "JAVA";

        Map<String, Double> simHashMap = new HashMap<>();

        System.out.println("======================================");
        long startTime = System.currentTimeMillis();
        System.out.println("原始标题：" + originalTitle);

        // 计算相似度
        titleList.forEach(title -> {
            SimHashUtil mySimHash_1 = new SimHashUtil(title, 64);
            SimHashUtil mySimHash_2 = new SimHashUtil(originalTitle, 64);

            Double similar = mySimHash_1.getSimilar(mySimHash_2);

            simHashMap.put(title, similar);
        });

        // 打印测试结果到控制台
        /* simHashMap.forEach((title, similarity) -> {
            System.out.println("标题："+title+"-----------相似度："+similarity);
        });*/

        // 按相标题内容排序输出控制台
        Set<String> titleSet = simHashMap.keySet();
        Object[] titleArrays = titleSet.toArray();
        Arrays.sort(titleArrays, Collections.reverseOrder());

        System.out.println("-------------------------------------");
        for (Object title : titleArrays) {
            System.out.println("标题：" + title + "-----------相似度：" + simHashMap.get(title));
        }

        // 求得运算时长（单位：毫秒）
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.out.println("\n本次运算总耗时" + totalTime + "毫秒");

        System.out.println("======================================");
    }
}
