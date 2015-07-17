package nl.knaw.huygens.alexandria.resource;

import nl.knaw.huygens.Log;
import nl.knaw.huygens.concordion.AlexandriaFixture;
import nl.knaw.huygens.alexandria.endpoint.resource.ResourcesEndpoint;
import org.junit.BeforeClass;

public class ResourcesBase extends AlexandriaFixture {
  @BeforeClass
  public static void registerEndpoint() {
    Log.trace("Registering ResourcesEndpoint");
    register(ResourcesEndpoint.class);
  }
}
