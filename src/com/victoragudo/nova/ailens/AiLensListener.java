package com.victoragudo.nova.ailens;

import com.intellij.util.messages.Topic;

public interface AiLensListener {

    Topic<AiLensListener> TOPIC = Topic.create("Nova AI Lens", AiLensListener.class);

    void spansChanged();
}
