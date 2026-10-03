package cc.asylum.iridium.core;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "cc.asylum.iridium.core", importOptions = ImportOption.DoNotIncludeTests.class)
final class ArchitectureTest {

  @ArchTest
  static final ArchRule productionCodeDoesNotDependOnTheTestStack = noClasses()
      .should().dependOnClassesThat().resideInAnyPackage("org.junit..", "org.mockito..")
      .because("production code must not depend on the test stack");
}
