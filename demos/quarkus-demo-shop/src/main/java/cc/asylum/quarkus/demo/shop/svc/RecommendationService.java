package cc.asylum.quarkus.demo.shop.svc;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class RecommendationService {

  private final String label;

  public RecommendationService(@ConfigProperty(name = "shop.recommendation", defaultValue = "on") final String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public int weight() {
    return 17;
  }
}
