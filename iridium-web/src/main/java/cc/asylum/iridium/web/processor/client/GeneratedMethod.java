package cc.asylum.iridium.web.processor.client;

import cc.asylum.forgery.member.MethodBuilder;

import java.util.function.Consumer;

public record GeneratedMethod(String name, Consumer<MethodBuilder> configure) {
}
