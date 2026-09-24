package cc.asylum.iridium.codegen.support;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelSupportTest {

  @Test
  void commonPrefixOfTwoPackages() {
    assertEquals("a.b", ModelSupport.commonPrefix("a.b.c", "a.b.d"));
  }

  @Test
  void commonPrefixWithoutOverlap() {
    assertEquals("", ModelSupport.commonPrefix("a.b", "c.d"));
  }

  @Test
  void commonPrefixOfSet() {
    assertEquals("com.example", ModelSupport.commonPrefix(Set.of("com.example.a", "com.example.b")));
  }

  @Test
  void commonPrefixOfEmptySet() {
    assertEquals("", ModelSupport.commonPrefix(Set.of()));
  }

  @Test
  void decapitalizesNames() {
    assertEquals("testService", ModelSupport.decapitalize("TestService"));
    assertEquals("a", ModelSupport.decapitalize("A"));
    assertEquals("", ModelSupport.decapitalize(""));
    assertEquals("uRL", ModelSupport.decapitalize("URL"));
  }

  @Test
  void commonPrefixKeepsTheShorterPackage() {
    assertEquals("a.b", ModelSupport.commonPrefix("a.b", "a.b.c"));
    assertEquals("a.b", ModelSupport.commonPrefix("a.b", "a.b"));
    assertEquals("cc.asylum.iridium", ModelSupport.rootPackage());
  }
}
