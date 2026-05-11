package com.group02.tars.service.impl;

import com.group02.tars.service.ServiceException;

import java.io.IOException;
import java.util.Map;

interface AiProvider {
    boolean isReady();

    Map<String, Object> status();

    AiProviderResult complete(String systemPrompt, String userPrompt, int maxTokens)
        throws IOException, ServiceException;
}
