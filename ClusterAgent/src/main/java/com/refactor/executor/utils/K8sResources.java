package com.refactor.executor.utils;

import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Service;

import java.util.List;

public class K8sResources {
    public List<V1Deployment> deployments;
    public List<V1Service> services;

    public K8sResources(List<V1Deployment> deployments, List<V1Service> services) {
        this.deployments = deployments;
        this.services = services;
    }
}