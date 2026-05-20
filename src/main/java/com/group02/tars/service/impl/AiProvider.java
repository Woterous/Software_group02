package com.group02.tars.service.impl;

import com.group02.tars.service.ServiceException;

import java.io.IOException;
import java.util.Map;

interface AiProvider {
    boolean isReady();

    Map<String, Object> status();

    AiProviderResult complete(String systemPrompt, String userPrompt, int maxTokens)
        throws IOException, ServiceException;

    default AiProviderResult completeWithFile(String systemPrompt, String userPrompt, AiFileInput file, int maxTokens)
        throws IOException, ServiceException {
        return complete(systemPrompt, userPrompt, maxTokens);
    }
}

record AiFileInput(String fileName, String mimeType, String dataUrl) {
}
