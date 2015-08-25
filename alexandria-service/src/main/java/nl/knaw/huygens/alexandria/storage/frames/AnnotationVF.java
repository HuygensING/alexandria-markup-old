package nl.knaw.huygens.alexandria.storage.frames;

import java.util.List;

import nl.knaw.huygens.alexandria.storage.Labels;
import peapod.annotations.Edge;
import peapod.annotations.In;
import peapod.annotations.Out;
import peapod.annotations.Vertex;

@Vertex(Labels.ANNOTATION)
public abstract class AnnotationVF extends AlexandriaVF {
  public static final String NO_VALUE = ":null";
  // TODO: double-check if (update of) peapod supports outgoing edges with the same label to different types of VF
  private static final String ANNOTATES_ANNOTATION = "annotates_annotation";
  private static final String DEPRECATES = "deprecates";
  static final String ANNOTATES_RESOURCE = "annotates_resource";
  static final String HAS_BODY = "has_body";

  public abstract void setRevision(Integer revision);

  public abstract Integer getRevision();

  @Out
  @Edge(HAS_BODY)
  public abstract AnnotationBodyVF getBody();

  @Out
  @Edge(HAS_BODY)
  public abstract void setBody(AnnotationBodyVF body);

  @Out
  @Edge(ANNOTATES_ANNOTATION)
  public abstract AnnotationVF getAnnotatedAnnotation();

  @Out
  @Edge(ANNOTATES_ANNOTATION)
  public abstract void setAnnotatedAnnotation(AnnotationVF annotationToAnnotate);

  @Out
  @Edge(ANNOTATES_RESOURCE)
  public abstract ResourceVF getAnnotatedResource();

  @Out
  @Edge(ANNOTATES_RESOURCE)
  public abstract void setAnnotatedResource(ResourceVF resourceToAnnotate);

  @In
  @Edge(AnnotationVF.ANNOTATES_ANNOTATION)
  public abstract List<AnnotationVF> getAnnotatedBy();

  @Out
  @Edge(AnnotationVF.DEPRECATES)
  public abstract void setDeprecatedAnnotation(AnnotationVF annotationToDeprecate);

  @Out
  @Edge(AnnotationVF.DEPRECATES)
  public abstract AnnotationVF getDeprecatedAnnotation();

  @In
  @Edge(AnnotationVF.DEPRECATES)
  public abstract AnnotationVF getDeprecatedBy();

  public String getType() {
    return getBody().getType();
  }

  public String getValue() {
    return getBody().getValue();
  }

  public String getResourceId() {
    ResourceVF annotatedResource = getFirstAnnotatedResource();
    if (annotatedResource != null) {
      ResourceVF parentResource = annotatedResource.getParentResource();
      return parentResource == null ? annotatedResource.getUuid() : parentResource.getUuid();
    }
    return NO_VALUE;
  }

  public String getSubResourceId() {
    ResourceVF annotatedResource = getFirstAnnotatedResource();
    if (annotatedResource != null && annotatedResource.isSubresource()) {
      return annotatedResource.getUuid();
    }
    return NO_VALUE;
  }

  private ResourceVF getFirstAnnotatedResource() {
    AnnotationVF annotation = findActiveVersionOf(this);
    while (annotation.getAnnotatedResource() == null) {
      annotation = findActiveVersionOf(annotation.getAnnotatedAnnotation());
    }
    return annotation.getAnnotatedResource();
  }

  private AnnotationVF findActiveVersionOf(AnnotationVF annotation) {
    while (annotation.isDeprecated()) {
      annotation = annotation.getDeprecatedAnnotation();
    }
    return annotation;
  }
}
