package nl.knaw.huygens.alexandria.endpoint.resource;

import nl.knaw.huygens.alexandria.api.EndpointPaths;
import nl.knaw.huygens.alexandria.api.model.text.TextRangeAnnotation;
import nl.knaw.huygens.alexandria.api.model.text.TextRangeAnnotationInfo;
import nl.knaw.huygens.alexandria.api.model.text.TextRangeAnnotationList;
import nl.knaw.huygens.alexandria.endpoint.JSONEndpoint;
import nl.knaw.huygens.alexandria.endpoint.LocationBuilder;
import nl.knaw.huygens.alexandria.endpoint.UUIDParam;
import nl.knaw.huygens.alexandria.exception.NotFoundException;
import nl.knaw.huygens.alexandria.model.AlexandriaResource;
import nl.knaw.huygens.alexandria.service.AlexandriaService;
import nl.knaw.huygens.alexandria.textgraph.TextGraphUtil;
import nl.knaw.huygens.alexandria.textgraph.TextRangeAnnotationValidatorFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

public class ResourceTextAnnotationEndpoint extends JSONEndpoint {

  private LocationBuilder locationBuilder;
  private AlexandriaService service;
  private AlexandriaResource resource;
  private UUID resourceUUID;
  private TextRangeAnnotationValidatorFactory textRangeAnnotationValidator;

  @Inject
  public ResourceTextAnnotationEndpoint(AlexandriaService service, //
      ResourceValidatorFactory validatorFactory, //
      LocationBuilder locationBuilder, //
      @PathParam("uuid") final UUIDParam uuidParam) {
    this.service = service;
    this.locationBuilder = locationBuilder;
    this.resource = validatorFactory.validateExistingResource(uuidParam).notTentative().hasText().get();
    this.resourceUUID = resource.getId();
    this.textRangeAnnotationValidator = new TextRangeAnnotationValidatorFactory(service, resourceUUID);
  }

  @GET
  public Response getAnnotations() {
    TextRangeAnnotationList textRangeAnnotations = service.readTextRangeAnnotations(resourceUUID);
    return ok(textRangeAnnotations);
  }

  @PUT
  @Path("{annotationUUID}")
  @Consumes(MediaType.APPLICATION_JSON)
  // TODO: clean this up, some concerns need separation
  public Response setAnnotation(//
      @PathParam("annotationUUID") final UUIDParam uuidParam, //
      @NotNull TextRangeAnnotation newTextRangeAnnotation//
  ) {
    UUID annotationUUID = uuidParam.getValue();
    newTextRangeAnnotation.setId(annotationUUID);

    String xml = getXML();
    String annotated = TextRangeAnnotationValidatorFactory.getAnnotatedText(newTextRangeAnnotation.getPosition(), xml);
    textRangeAnnotationValidator.calculateAbsolutePosition(newTextRangeAnnotation, annotated);

    Optional<TextRangeAnnotation> existingTextRangeAnnotation = service.readTextRangeAnnotation(resourceUUID, annotationUUID);
    if (existingTextRangeAnnotation.isPresent()) {
      textRangeAnnotationValidator.validate(newTextRangeAnnotation, existingTextRangeAnnotation.get(), xml);
      service.setTextRangeAnnotation(resourceUUID, newTextRangeAnnotation);
      return noContent();
    }

    textRangeAnnotationValidator.validate(newTextRangeAnnotation, xml);
    TextRangeAnnotationInfo info = new TextRangeAnnotationInfo().setAnnotates(annotated);

    service.setTextRangeAnnotation(resourceUUID, newTextRangeAnnotation);
    URI location = locationBuilder.locationOf(resource, EndpointPaths.TEXT, EndpointPaths.ANNOTATIONS, annotationUUID.toString());
    return Response.created(location).entity(info).build();
  }

  @GET
  @Path("{annotationUUID}")
  public Response getAnnotation(@PathParam("annotationUUID") final UUIDParam uuidParam) {
    UUID annotationUUID = uuidParam.getValue();
    TextRangeAnnotation textRangeAnnotation = service.readTextRangeAnnotation(resourceUUID, annotationUUID)//
        .orElseThrow(() -> new NotFoundException("No annotation found for this resource with id " + annotationUUID));
    return ok(textRangeAnnotation);
  }

  private String getXML() {
    StreamingOutput outputStream = TextGraphUtil.streamXML(service, resourceUUID);
    return TextGraphUtil.asString(outputStream);
  }

}
