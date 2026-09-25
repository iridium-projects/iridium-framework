package cc.asylum.quarkus.demo.shop.svc;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MediaService {

  private final String label;

  public MediaService(@ConfigProperty(name = "shop.media", defaultValue = "on") final String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public int weight() {
    return 34;
  }
}
