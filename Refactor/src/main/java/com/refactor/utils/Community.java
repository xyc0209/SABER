package com.refactor.utils;

import com.refactor.chain.utils.Edge;
import com.refactor.chain.utils.Node;

import java.util.*;
import java.util.stream.Collectors;

public class Community {
    Set<Node> members;

    Map<Node, Set<Node>> parentCountMap;
    public Community() {
        this.members = new HashSet<>();
        this.parentCountMap = new HashMap<>();
    }

    public Set<Node> getMembers() {
        return members;
    }

    public void setMembers(Set<Node> members) {
        this.members = members;
    }

    public Map<Node, Set<Node>> getParentCountMap() {
        return parentCountMap;
    }

    public List<Node> getAllEdges(){
        List<Node> nodes = new ArrayList<>();
        for (Node node: members){
            for(Edge edge: node.getEdges()){
                nodes.add(edge.getTarget());
            }
        }
        return nodes;
    }

    public void setParentCountMap(Map<Node, Set<Node>> parentCountMap) {
        this.parentCountMap = parentCountMap;
    }

    public boolean isValid(boolean ifExistSvcLayer) {
        boolean hasController = false;
        boolean hasServiceImpl = false;
        boolean hasRepository = false;
        boolean hasMapper = false;
        boolean hasEntity = false;

        for (Node node : members) {
            switch (node.getType()) {
                case CONTROLLER:
                    hasController = true;
                    break;
                case SERVICE:
                    hasServiceImpl = true;
                    break;
                case MAPPER:
                    hasMapper = true;
                case REPOSITORY:
                    hasRepository = true;
                    break;
                case ENTITY:
                    hasEntity = true;
                    break;
            }
        }
        if (ifExistSvcLayer)
            return (hasController && hasServiceImpl && hasRepository && hasEntity) || (hasController && hasServiceImpl && hasMapper && hasEntity);
        else
            return hasController;
    }



    @Override
    public String toString() {
        return "Community{" +
                "members=" + members.stream().map(n -> n.getId()).collect(Collectors.toSet()) +
                '}';
    }


}
