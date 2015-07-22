package nl.knaw.huygens.alexandria.endpoint.annotation;

import javax.inject.Inject;
import javax.ws.rs.PathParam;

import nl.knaw.huygens.Log;
import nl.knaw.huygens.alexandria.endpoint.AnnotatableObjectAnnotationsEndpoint;
import nl.knaw.huygens.alexandria.endpoint.AnnotationCreationRequestBuilder;
import nl.knaw.huygens.alexandria.endpoint.LocationBuilder;
import nl.knaw.huygens.alexandria.endpoint.UUIDParam;
import nl.knaw.huygens.alexandria.model.AbstractAnnotatable;
import nl.knaw.huygens.alexandria.model.AlexandriaAnnotation;
import nl.knaw.huygens.alexandria.service.AlexandriaService;

public class AnnotationAnnotationsEndpoint extends AnnotatableObjectAnnotationsEndpoint {

  // TODO: how to remove this duplicated inject/constructor?
  @Inject
  public AnnotationAnnotationsEndpoint(AlexandriaService service, //
      AnnotationCreationRequestBuilder requestBuilder, //
      LocationBuilder locationBuilder, //
      @PathParam("uuid") final UUIDParam uuidParam) {
    super(service, requestBuilder, locationBuilder, uuidParam);
    Log.trace("resourceService=[{}], uuidParam=[{}]", service, uuidParam);
  }

  @Override
  protected AbstractAnnotatable getAnnotatableObject() {
    AlexandriaAnnotation annotation = service.readAnnotation(uuid)//
        .orElseThrow(AnnotationsEndpoint.annotationNotFoundForId(uuid));
    if (annotation.isTentative()){
      throw AnnotationsEndpoint.annotationIsTentative(uuid);
    }
    return annotation;
  }

  @Override
  protected AnnotationCreationRequestBuilder getAnnotationCreationRequestBuilder() {
    return requestBuilder.ofAnnotation(uuid);
  }

}
