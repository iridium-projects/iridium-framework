package cc.asylum.iridium.codegen;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "cc.asylum.iridium.codegen", importOptions = ImportOption.DoNotIncludeTests.class)
final class ArchitectureTest {

  @ArchTest
  static final ArchRule codegenDoesNotDependOnRuntimeModules = noClasses()
      .that().resideInAPackage("cc.asylum.iridium.codegen..")
      .should().dependOnClassesThat().resideInAnyPackage(
          "cc.asylum.iridium.core..",
          "cc.asylum.iridium.web..",
          "cc.asylum.iridium.config..",
          "cc.asylum.iridium.log..")
      .because("iridium-codegen is a compile-time module");
}
