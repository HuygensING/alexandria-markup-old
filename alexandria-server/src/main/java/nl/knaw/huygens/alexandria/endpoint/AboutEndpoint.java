package nl.knaw.huygens.alexandria.endpoint;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.PropertyResourceBundle;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.google.common.collect.Maps;

@Singleton
@Path("about")
public class AboutEndpoint extends JSONEndpoint {
  Date starttime = new Date();

  /**
   * Show information about the back-end
   *
   * @return about-data map
   */
  @GET
  public Response getMetadata() {
    Map<String, String> data = Maps.newLinkedHashMap();
    data.put("version", getProperty("version"));
    data.put("builddate", getProperty("builddate"));
    data.put("started", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(this.starttime));
    return Response.ok(data).build();
  }

  // private methods
  private static PropertyResourceBundle propertyResourceBundle;

  private static synchronized String getProperty(String key) {
    if (propertyResourceBundle == null) {
      try {
        propertyResourceBundle = new PropertyResourceBundle(//
            Thread.currentThread().getContextClassLoader().getResourceAsStream("about.properties"));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return propertyResourceBundle.getString(key);
  }

}
