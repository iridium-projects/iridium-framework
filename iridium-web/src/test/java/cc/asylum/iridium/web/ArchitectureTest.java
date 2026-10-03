package cc.asylum.iridium.web;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "cc.asylum.iridium.web", importOptions = ImportOption.DoNotIncludeTests.class)
final class ArchitectureTest {

  @ArchTest
  static final ArchRule webDoesNotDependOnConfig = noClasses()
      .that().resideInAPackage("cc.asylum.iridium.web..")
      .should().dependOnClassesThat().resideInAPackage("cc.asylum.iridium.config..")
      .because("iridium-web must not depend on iridium-config");
}
