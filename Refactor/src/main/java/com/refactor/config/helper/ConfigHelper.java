package com.refactor.config.helper;

import com.refactor.config.DependenciesConfig;
import com.refactor.config.TemplatesConfig;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Cocoicobird
 * @version 1.0
 */
@Component
public class ConfigHelper {

    @Getter
    private static TemplatesConfig templatesConfig;

    @Getter
    private static DependenciesConfig dependenciesConfig;

    public ConfigHelper(@Autowired TemplatesConfig templatesConfig, @Autowired DependenciesConfig dependenciesConfig) {
        ConfigHelper.templatesConfig = templatesConfig;
        ConfigHelper.dependenciesConfig = dependenciesConfig;
    }
}
