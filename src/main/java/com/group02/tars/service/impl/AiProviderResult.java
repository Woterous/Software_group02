package com.group02.tars.service.impl;

import java.util.Map;

record AiProviderResult(String content, String reasoningContent, String model, String finishReason, Map<String, Object> usage) {
}
