package nl.knaw.huygens.alexandria.endpoint.resource;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import java.net.URI;
import java.util.Optional;

import nl.knaw.huygens.alexandria.endpoint.EndpointPaths;
import nl.knaw.huygens.alexandria.endpoint.JSONEndpoint;
import nl.knaw.huygens.alexandria.endpoint.UUIDParam;
import nl.knaw.huygens.alexandria.model.AlexandriaResource;
import nl.knaw.huygens.alexandria.service.ResourceService;

@Path(EndpointPaths.RESOURCES)
public class ResourcesEndpoint extends JSONEndpoint {
  private static final URI HERE = URI.create("");

  private final ResourceService resourceService;
  private final ResourceEntityBuilder entityBuilder;
  private final ResourceCreationCommandBuilder commandBuilder;

  @Inject
  public ResourcesEndpoint(ResourceService resourceService, //
                           ResourceCreationCommandBuilder commandBuilder, //
                           ResourceEntityBuilder entityBuilder) {
    log().trace("Resources created, resourceService=[{}]", resourceService);

    this.entityBuilder = entityBuilder;
    this.commandBuilder = commandBuilder;
    this.resourceService = resourceService;
  }

  @GET
  @Path("{uuid}")
  public Response getResourceByID(@PathParam("uuid") final UUIDParam uuid) {
    final AlexandriaResource resource = resourceService.readResource(uuid.getValue());
    return Response.ok(entityBuilder.build(resource)).build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createResourceWithoutGivenID(ResourcePrototype protoType) {
    log().trace("protoType=[{}]", protoType);
    requireProtoType(protoType);

    final ResourceCreationCommand command = commandBuilder.withoutId(protoType);
    final AlexandriaResource resource = command.execute(resourceService);

    if (command.wasExecutedAsIs()) {
      return Response.noContent().build();
    }

    final ResourceEntity entity = entityBuilder.build(resource);
    return Response.created(locationOf(resource)).entity(entity).build();
  }

  @PUT
  @Path("{uuid}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createResourceAtSpecificID(@PathParam("uuid") final UUIDParam paramId, ResourcePrototype protoType) {
    log().trace("paramId=[{}], protoType=[{}]", paramId, protoType);
    requireProtoType(protoType);

    final ResourceCreationCommand command = commandBuilder.ofSpecificId(protoType, paramId.getValue());
    final AlexandriaResource resource = command.execute(resourceService);

    final ResponseBuilder builder;
    if (command.newResourceWasCreated()) {
      builder = Response.created(HERE).entity(entityBuilder.build(resource));
    } else {
      if (command.wasExecutedAsIs()) {
        builder = Response.noContent();
      } else {
        builder = Response.ok(entityBuilder.build(resource));
      }
    }

    return builder.build();
  }

  @DELETE
  @Path("{uuid}")
  public Response deleteNotSupported(@PathParam("uuid") final UUIDParam paramId) {
    return Response.status(501).build();
  }

  // TODO: replace with sub-resource analogous to {uuid}/annotations (see below)
  @GET
  @Path("{uuid}/ref")
  public Response getResourceRef(@PathParam("uuid") final UUIDParam uuidParam) {
    AlexandriaResource resource = resourceService.readResource(uuidParam.getValue());
    return Response.ok(new RefEntity(resource.getRef())).build();
  }

  // Sub-resource delegation

  @Path("{uuid}/annotations")
  public Class<ResourceAnnotations> getResourceAnnotations() {
    return ResourceAnnotations.class; // no instantiation of our own; let Jersey handle the lifecycle
  }

  // TODO: remove by using Jersey's Bean Validation
  private void requireProtoType(ResourcePrototype protoType) {
    Optional.ofNullable(protoType).orElseThrow(missingBodyException());
  }

  // TODO: replace by injected LocationBuilder (to be written) ?
  private URI locationOf(AlexandriaResource resource) {
    return URI.create(resource.getId().toString());
  }

}
