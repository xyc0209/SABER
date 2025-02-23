package com.refactor.config.convert;

import org.apache.maven.model.Dependency;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Cocoicobird
 * @version 1.0
 */
@Component
@ConfigurationPropertiesBinding
public class DependencyConverter implements Converter<Map<String, String>, Dependency> {
    @Override
    public Dependency convert(Map<String, String> source) {
        Dependency dependency = new Dependency();
        if (source.containsKey("groupId"))
            dependency.setGroupId(source.get("groupId"));
        if (source.containsKey("artifactId"))
            dependency.setArtifactId(source.get("artifactId"));
        if (source.containsKey("version"))
            dependency.setVersion(source.get("version"));
        if (source.containsKey("type"))
            dependency.setScope(source.get("type"));
        if (source.containsKey("scope"))
            dependency.setScope(source.get("scope"));
        return dependency;
    }
}
