package com.group02.tars.service.impl;

import java.util.Map;

/**
 * Normalized result returned by an AI provider call.
 *
 * @param content primary response content
 * @param reasoningContent optional provider reasoning content
 * @param model model name returned by the provider
 * @param finishReason provider finish reason
 * @param usage provider usage metadata
 */
record AiProviderResult(String content, String reasoningContent, String model, String finishReason, Map<String, Object> usage) {
}
