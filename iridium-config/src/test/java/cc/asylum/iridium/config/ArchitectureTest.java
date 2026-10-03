package cc.asylum.iridium.config;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "cc.asylum.iridium.config", importOptions = ImportOption.DoNotIncludeTests.class)
final class ArchitectureTest {

  @ArchTest
  static final ArchRule configDoesNotDependOnWeb = noClasses()
      .that().resideInAPackage("cc.asylum.iridium.config..")
      .should().dependOnClassesThat().resideInAPackage("cc.asylum.iridium.web..")
      .because("iridium-config must not depend on iridium-web");
}
