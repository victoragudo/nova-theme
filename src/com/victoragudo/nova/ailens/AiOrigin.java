package com.victoragudo.nova.ailens;

public enum AiOrigin {
    ASSISTANT("an AI assistant in the IDE"),
    AGENT("an external AI agent"),
    MANUAL("you, marked manually");

    private final String description;

    AiOrigin(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

    static AiOrigin parse(String name) {
        for (AiOrigin origin : values()) {
            if (origin.name().equals(name)) {
                return origin;
            }
        }
        return MANUAL;
    }
}
