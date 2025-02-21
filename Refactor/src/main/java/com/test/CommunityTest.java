package com.test;

import java.util.ArrayList;
import java.util.List;

class Node {
    int id;

    public Node(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Node)) return false;
        Node other = (Node) obj;
        return this.id == other.id; // 根据 id 比较
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id); // 重写 hashCode
    }
}

class Community {
    List<Node> members = new ArrayList<>();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Community)) return false;
        Community other = (Community) obj;
        return this.members.equals(other.members); // 根据成员比较
    }

    @Override
    public int hashCode() {
        return members.hashCode(); // 重写 hashCode
    }

    @Override
    public String toString() {
        return "Community{" + "members=" + members + '}';
    }
}

public class CommunityTest {
    public static void main(String[] args) {
        // 创建 communities 列表并添加四个元素
        List<Community> communities = new ArrayList<>();

        Community community1 = new Community();
        community1.members.add(new Node(1));
        community1.members.add(new Node(2));
        communities.add(community1);

        Community community2 = new Community();
        community2.members.add(new Node(3));
        communities.add(community2);

        Community community3 = new Community();
        community3.members.add(new Node(4));
        communities.add(community3);

        Community community4 = new Community();
        community4.members.add(new Node(5));
        communities.add(community4);

        // 打印原始 communities
        System.out.println("Original communities:");
        for (Community community : communities) {
            System.out.println(community);
        }

        // 创建 toRemove 列表并添加两个元素
        List<Community> toRemove = new ArrayList<>();
        toRemove.add(community1); // 添加 community1
        toRemove.add(community2); // 添加 community2

        // 从 communities 中删除 toRemove 中的元素
        communities.removeAll(toRemove);

        // 打印删除后的 communities
        System.out.println("\nCommunities after removal:");
        for (Community community : communities) {
            System.out.println(community);
        }
    }
}