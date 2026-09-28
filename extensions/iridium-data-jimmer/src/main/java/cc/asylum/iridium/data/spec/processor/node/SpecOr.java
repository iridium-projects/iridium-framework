package cc.asylum.iridium.data.spec.processor.node;

import java.util.List;

public record SpecOr(List<SpecNode> children) implements SpecNode {
}
