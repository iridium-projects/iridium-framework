package cc.asylum.iridium.data.config;

import cc.asylum.iridium.core.component.Component;
import cc.asylum.iridium.data.DataClients;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

@Component
public final class PooledDataSource implements DataSource, AutoCloseable {

  private final HikariDataSource delegate;

  public PooledDataSource(final DataSettings settings) {
    this.delegate = (HikariDataSource) DataClients.dataSource(settings);
  }

  @Override
  public Connection getConnection() throws SQLException {
    return delegate.getConnection();
  }

  @Override
  public Connection getConnection(final String username, final String password) throws SQLException {
    return delegate.getConnection(username, password);
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return delegate.getLogWriter();
  }

  @Override
  public void setLogWriter(final PrintWriter out) throws SQLException {
    delegate.setLogWriter(out);
  }

  @Override
  public void setLoginTimeout(final int seconds) throws SQLException {
    delegate.setLoginTimeout(seconds);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return delegate.getLoginTimeout();
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return delegate.getParentLogger();
  }

  @Override
  public <T> T unwrap(final Class<T> iface) throws SQLException {
    return delegate.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(final Class<?> iface) throws SQLException {
    return delegate.isWrapperFor(iface);
  }

  @Override
  public void close() {
    delegate.close();
  }
}
