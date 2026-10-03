package cc.asylum.iridium.log;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "cc.asylum.iridium.log", importOptions = ImportOption.DoNotIncludeTests.class)
final class ArchitectureTest {

  @ArchTest
  static final ArchRule logDoesNotDependOnOtherModules = noClasses()
      .that().resideInAPackage("cc.asylum.iridium.log..")
      .should().dependOnClassesThat().resideInAnyPackage(
          "cc.asylum.iridium.core..",
          "cc.asylum.iridium.web..",
          "cc.asylum.iridium.config..",
          "cc.asylum.iridium.codegen..")
      .because("iridium-log is a leaf module");
}
