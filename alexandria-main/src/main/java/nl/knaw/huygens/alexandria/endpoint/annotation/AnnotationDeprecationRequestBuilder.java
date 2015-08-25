package nl.knaw.huygens.alexandria.endpoint.annotation;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.inject.Inject;

import nl.knaw.huygens.Log;
import nl.knaw.huygens.alexandria.exception.BadRequestException;
import nl.knaw.huygens.alexandria.model.AlexandriaAnnotation;
import nl.knaw.huygens.alexandria.service.AlexandriaService;

public class AnnotationDeprecationRequestBuilder {
  public static final String MISSING_TYPE_MESSAGE = "Annotation MUST have a type";
  public static final String NO_SUCH_ANNOTATION_FORMAT = "Supposedly existing annotation [%s] not found";
  public static final String MISSING_ANNOTATION_BODY_MESSAGE = "Missing or empty annotation request body";

  private final AlexandriaService service;
  private AlexandriaAnnotation originalAnnotation;

  @Inject
  public AnnotationDeprecationRequestBuilder(AlexandriaService service) {
    this.service = requireNonNull(service, "AlexandriaService MUST not be null");
  }

  public AnnotationDeprecationRequestBuilder ofAnnotation(AlexandriaAnnotation annotation) {
    this.originalAnnotation = annotation;
    return this;
  }

  public AnnotationDeprecationRequest build(AnnotationUpdatePrototype prototype) {
    return new AnnotationDeprecationRequest(originalAnnotation, prototype);
  }

  protected void validateId(AnnotationPrototype prototype) {
    Log.trace("Validating id");
    return; // missing or empty Id means client does not override server
    // generated value.
  }

  protected void validateType(AnnotationPrototype prototype) {
    Log.trace("Validating type");
    // prototype.getType().filter(s -> !s.isEmpty()).orElseThrow(missingTypeException());
  }

  protected void validateValue(AnnotationPrototype prototype) {
    Log.trace("Validating value");
    return; // missing or empty value is ok (means annotation is a Tag)
  }

  protected void validateProvenance(AnnotationPrototype prototype) {
    Log.trace("Validating provenance");
    return; // missing or empty provenance means client does not override server
    // set value.
  }

  // protected void validateAnnotations(AnnotationPrototype prototype) {
  // Log.trace("Validating annotations");
  // prototype.getAnnotations().ifPresent(annotationParams ->
  // stream(annotationParams) //
  // .map(UUIDParam::getValue).forEach(this::validateAnnotationId));
  // }

  protected <T> Stream<T> stream(Collection<T> c) {
    return c.parallelStream(); // override in case you prefer stream() over
    // parallelStream()
  }

  protected void validateAnnotationId(UUID uuid) {
    Log.trace("Validating annotation: [{}]", uuid);
    Optional.ofNullable(service.readAnnotation(uuid)).orElseThrow(noSuchAnnotationException(uuid));
  }

  protected Supplier<BadRequestException> missingBodyException() {
    return () -> badRequestException(MISSING_ANNOTATION_BODY_MESSAGE);
  }

  protected Supplier<BadRequestException> missingTypeException() {
    return () -> badRequestException(MISSING_TYPE_MESSAGE);
  }

  protected Supplier<BadRequestException> noSuchAnnotationException(UUID uuid) {
    return () -> badRequestException(String.format(NO_SUCH_ANNOTATION_FORMAT, uuid.toString()));
  }

  protected BadRequestException badRequestException(String message) {
    Log.trace(message);
    return new BadRequestException(message);
  }

}
