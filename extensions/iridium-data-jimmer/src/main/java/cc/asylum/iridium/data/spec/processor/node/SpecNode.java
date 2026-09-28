package cc.asylum.iridium.data.spec.processor.node;

public sealed interface SpecNode permits SpecLeaf, SpecAnd, SpecOr, SpecNot {
}
