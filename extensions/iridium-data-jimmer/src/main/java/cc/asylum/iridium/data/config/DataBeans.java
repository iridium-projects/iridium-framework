package cc.asylum.iridium.data.config;

import cc.asylum.iridium.core.bean.Bean;
import cc.asylum.iridium.core.hook.ShutdownHook;
import cc.asylum.iridium.data.DataClients;
import org.babyfish.jimmer.sql.JSqlClient;

import javax.sql.DataSource;

public final class DataBeans {

  @Bean
  public JSqlClient sqlClient(final DataSource dataSource, final DataSettings settings) {
    return DataClients.sqlClient(dataSource, settings);
  }

  @Bean
  public ShutdownHook dataShutdown(final DataSource dataSource) {
    return new ShutdownHook() {
      @Override
      public void run() {
        if (dataSource instanceof final AutoCloseable closeable) {
          try {
            closeable.close();
          } catch (final Exception exception) {
            throw new IllegalStateException("Failed to close data source", exception);
          }
        }
      }

      @Override
      public int priority() {
        return 1_000;
      }
    };
  }
}
