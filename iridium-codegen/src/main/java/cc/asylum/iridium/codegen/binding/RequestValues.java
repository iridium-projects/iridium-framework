package cc.asylum.iridium.codegen.binding;

import cc.asylum.iridium.core.annotation.Internal;

@Internal
public interface RequestValues {

  String one(boolean header, String name, String fallback);

  String many(boolean header, String name);

  String invalid(String rawVariable, String param);
}
