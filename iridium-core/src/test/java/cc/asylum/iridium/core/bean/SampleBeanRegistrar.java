package cc.asylum.iridium.core.bean;

public final class SampleBeanRegistrar implements BeanRegistrar {

  @Override
  public void register(final BeanPool pool) {
    pool.put("sample", "from-service");
  }
}
