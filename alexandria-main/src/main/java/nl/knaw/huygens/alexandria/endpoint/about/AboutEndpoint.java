package nl.knaw.huygens.alexandria.endpoint.about;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.google.common.collect.Maps;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import nl.knaw.huygens.Log;
import nl.knaw.huygens.alexandria.endpoint.JSONEndpoint;
import nl.knaw.huygens.alexandria.service.AlexandriaService;

@Singleton
@Path("about")
@Api("about")
public class AboutEndpoint extends JSONEndpoint {
  Date starttime = new Date();

  @Inject
  AlexandriaService s;

  /**
   * Show information about the back-end
   *
   * @return about-data map
   */
  @GET
  @ApiOperation("get information about the server (version,builddate,started)")
  public Response getMetadata() {
    Log.info("service={}", s);
    Map<String, String> data = Maps.newLinkedHashMap();
    data.put("version", getProperty("version"));
    data.put("builddate", getProperty("builddate"));
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("AMS"));
    data.put("started", simpleDateFormat.format(this.starttime));
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