package com.refactor.chain.utils.toJson;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class Point {
    private final String name;
    private final List<Point> children = new ArrayList<>();

    public Point(String name) {
        this.name = name;
    }

    public void addChild(Point child) {
        children.add(child);
    }

    public String getName() {
        return name;
    }

    public List<Point> getChildren() {
        return children;
    }

    public void deleteChild(Point child) {
        children.remove(child);
    }

    @JsonIgnore
    public int getChildrenSize() {
        return children.size();
    }

    public String toJson() throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        return om.writeValueAsString(this);
    }


}
