package cc.asylum.iridium.log;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.BasicMDCAdapter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

public final class LogProvider implements SLF4JServiceProvider {

  private final IMarkerFactory markers = new BasicMarkerFactory();
  private final MDCAdapter mdc = new BasicMDCAdapter();
  private LoggerFactory factory;

  @Override
  public void initialize() {
    factory = new LoggerFactory();
  }

  @Override
  public ILoggerFactory getLoggerFactory() {
    return factory;
  }

  @Override
  public IMarkerFactory getMarkerFactory() {
    return markers;
  }

  @Override
  public MDCAdapter getMDCAdapter() {
    return mdc;
  }

  @Override
  public String getRequestedApiVersion() {
    return "2.0";
  }
}
