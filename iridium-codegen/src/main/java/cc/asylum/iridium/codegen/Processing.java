package cc.asylum.iridium.codegen;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public record Processing(
    Types types,
    Elements elements,
    Messager messager,
    Filer filer,
    RoundEnvironment round
) {
}
