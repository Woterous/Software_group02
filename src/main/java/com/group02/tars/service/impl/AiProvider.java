package com.group02.tars.service.impl;

import com.group02.tars.service.ServiceException;

import java.io.IOException;
import java.util.Map;

/**
 * Minimal adapter interface used by the AI assistant service to call a model provider.
 */
interface AiProvider {
    /**
     * Reports whether the provider has enough configuration to make model calls.
     *
     * @return {@code true} when model calls can be attempted
     */
    boolean isReady();

    /**
     * Returns provider status metadata for API responses.
     *
     * @return provider status fields
     */
    Map<String, Object> status();

    /**
     * Completes a text-only prompt.
     *
     * @param systemPrompt system instruction prompt
     * @param userPrompt user task prompt
     * @param maxTokens requested maximum output tokens
     * @return provider completion result
     * @throws IOException if the provider request or response cannot be processed
     * @throws ServiceException if the provider is unavailable or returns an invalid response
     */
    AiProviderResult complete(String systemPrompt, String userPrompt, int maxTokens)
        throws IOException, ServiceException;

    /**
     * Completes a prompt with an optional file input.
     *
     * @param systemPrompt system instruction prompt
     * @param userPrompt user task prompt
     * @param file optional file input
     * @param maxTokens requested maximum output tokens
     * @return provider completion result
     * @throws IOException if the provider request or response cannot be processed
     * @throws ServiceException if the provider is unavailable or returns an invalid response
     */
    default AiProviderResult completeWithFile(String systemPrompt, String userPrompt, AiFileInput file, int maxTokens)
        throws IOException, ServiceException {
        return complete(systemPrompt, userPrompt, maxTokens);
    }
}

/**
 * Inline file payload passed to an AI provider.
 *
 * @param fileName submitted or stored file name
 * @param mimeType media type for the file
 * @param dataUrl file content encoded as a data URL
 */
record AiFileInput(String fileName, String mimeType, String dataUrl) {
}
