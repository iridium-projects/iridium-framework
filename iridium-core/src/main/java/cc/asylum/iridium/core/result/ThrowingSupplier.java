package cc.asylum.iridium.core.result;

@FunctionalInterface
public interface ThrowingSupplier<T, X extends Throwable> {

  T get() throws X;
}
