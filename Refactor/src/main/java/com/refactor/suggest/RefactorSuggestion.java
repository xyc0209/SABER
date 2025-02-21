package com.refactor.suggest;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @description: used for
 * @author: xyc
 * @date: 2024-11-22 17:39
 */
@Data
@Component
public class RefactorSuggestion {
    public List<RefactorSPSuggestion> refactorSPSuggestions;

    public List<RefactorISISuggestion> refactorISISuggestions;
}