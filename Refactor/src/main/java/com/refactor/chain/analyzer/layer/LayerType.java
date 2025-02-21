package com.refactor.chain.analyzer.layer;

public enum LayerType {
    ROOT,
    CONTROLLER,
    SERVICE,
    MAPPER,
    REPOSITORY,
    ENTITY,
    OTHER;

    public static boolean isValidLayerType(LayerType type) {
        return type == CONTROLLER || type == SERVICE || type == REPOSITORY  || type == MAPPER || type == ENTITY;
    }
}
