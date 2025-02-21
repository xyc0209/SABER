package com.refactor.chain.utils.toJson;

import com.fasterxml.jackson.core.JsonProcessingException;

public class PointTree {
    private Point root;

    public PointTree(Point root) {
        this.root = root;
    }

    public Point getRoot() {
        return root;
    }

    public void setRoot(Point root) {
        this.root = root;
    }

    public String toJson() throws JsonProcessingException {
        return root.toJson();
    }
}
