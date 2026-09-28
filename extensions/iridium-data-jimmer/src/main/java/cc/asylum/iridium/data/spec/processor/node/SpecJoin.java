package cc.asylum.iridium.data.spec.processor.node;

import javax.lang.model.type.TypeMirror;

public record SpecJoin(String path, String kind, TypeMirror target) {
}
