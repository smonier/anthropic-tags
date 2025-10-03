package org.jahia.se.modules.anthropic.service;

import org.json.JSONArray;

import java.util.List;

public interface AnthropicService {
    /**
     * generateAutoTagsfrom current node via Anthropic.
     * @param path node.path source
     * @param language langue source
     * @return List
     */
    List<String> generateAutoTags(String path, String language, String tagLanguage);
}

