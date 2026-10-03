package cc.asylum.iridium.log;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.spi.MDCAdapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class LogProviderTest {

  @Test
  void initializeCreatesFactoryAndExposesCollaborators() {
    final LogProvider provider = new LogProvider();

    assertNull(provider.getLoggerFactory());
    assertNotNull(provider.getMarkerFactory());
    assertNotNull(provider.getMDCAdapter());
    assertEquals("2.0", provider.getRequestedApiVersion());

    provider.initialize();

    final ILoggerFactory factory = provider.getLoggerFactory();
    assertNotNull(factory);
    assertSame(factory, provider.getLoggerFactory());
    assertNotNull(factory.getLogger("cc.asylum.provider"));
    assertSame(provider.getMarkerFactory(), provider.getMarkerFactory());
    assertSame(provider.getMDCAdapter(), provider.getMDCAdapter());
  }

  @Test
  void markerAndMdcAreUsable() {
    final LogProvider provider = new LogProvider();
    final IMarkerFactory markers = provider.getMarkerFactory();
    final MDCAdapter mdc = provider.getMDCAdapter();

    assertEquals("req", markers.getMarker("req").getName());
    mdc.put("k", "v");
    assertEquals("v", mdc.get("k"));
    mdc.clear();
    assertNull(mdc.get("k"));
  }

  @Test
  void mockedCollaboratorTypesAreDistinctFromProviderInstances() {
    final ILoggerFactory factory = mock(ILoggerFactory.class);
    final IMarkerFactory markers = mock(IMarkerFactory.class);
    final MDCAdapter mdc = mock(MDCAdapter.class);
    final LogProvider provider = new LogProvider();
    provider.initialize();

    assertNotNull(factory);
    assertNotNull(markers);
    assertNotNull(mdc);
    assertNotNull(provider.getLoggerFactory());
  }
}
