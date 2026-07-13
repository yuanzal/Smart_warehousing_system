package com.qst.smart_warehousing.service.Impl;

import java.util.*;

public class AStarPathFinder {

    // 定义节点
    public static class Node implements Comparable<Node> {
        public int x, y;
        public int g = 0; // 从起点到当前节点的代价
        public int h = 0; // 从当前节点到终点的启发式代价（曼哈顿距离）
        public int f = 0; // f = g + h
        public Node parent;

        public Node(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.f, other.f);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Node)) return false;
            Node node = (Node) o;
            return x == node.x && y == node.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    /**
     * A* 寻路核心算法
     * @param map 二维网格地图：0代表空闲，1代表障碍物（货架/墙壁）
     * @param startX 起点X
     * @param startY 起点Y
     * @param endX 终点X
     * @param endY 终点Y
     * @return 路径拓扑节点集合
     */
    public static List<int[]> findPath(int[][] map, int startX, int startY, int endX, int endY) {
        int rows = map.length;
        int cols = map[0].length;

        // 边界控制
        if (startX < 0 || startX >= rows || startY < 0 || startY >= cols ||
                endX < 0 || endX >= rows || endY < 0 || endY >= cols) {
            return Collections.emptyList();
        }

        PriorityQueue<Node> openList = new PriorityQueue<>();
        Set<Node> closedList = new HashSet<>();

        Node startNode = new Node(startX, startY);
        Node endNode = new Node(endX, endY);

        openList.add(startNode);

        // 四方向移动：上下左右
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        while (!openList.isEmpty()) {
            Node current = openList.poll();
            closedList.add(current);

            // 找到终点，开始回溯路径
            if (current.equals(endNode)) {
                List<int[]> path = new ArrayList<>();
                Node temp = current;
                while (temp != null) {
                    path.add(0, new int[]{temp.x, temp.y}); // 倒序插入
                    temp = temp.parent;
                }
                return path;
            }

            // 遍历邻居节点
            for (int[] dir : directions) {
                int nextX = current.x + dir[0];
                int nextY = current.y + dir[1];

                // 越界检查、障碍物检查
                if (nextX < 0 || nextX >= rows || nextY < 0 || nextY >= cols || map[nextX][nextY] == 1) {
                    continue;
                }

                Node neighbor = new Node(nextX, nextY);
                if (closedList.contains(neighbor)) {
                    continue;
                }

                int tentativeG = current.g + 1;

                // 启发式：曼哈顿距离
                neighbor.h = Math.abs(nextX - endX) + Math.abs(nextY - endY);
                neighbor.f = tentativeG + neighbor.h;
                neighbor.parent = current;
                neighbor.g = tentativeG;

                // 简化版的 openList 节点更新逻辑
                if (!openList.contains(neighbor)) {
                    openList.add(neighbor);
                }
            }
        }
        return Collections.emptyList(); // 没找到路径（被障碍物堵死）
    }
}