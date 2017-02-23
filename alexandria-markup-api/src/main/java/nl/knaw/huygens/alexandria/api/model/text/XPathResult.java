package nl.knaw.huygens.alexandria.api.model.text;


public class XPathResult {
  public enum Type {
    BOOLEAN, NUMBER, STRING, NODESET
  }

  Object result;
  XPathResult.Type type;

  public XPathResult(XPathResult.Type type, Object result) {
    this.type = type;
    this.result = result;
  }

  public Object getResult() {
    return result;
  }

  public XPathResult.Type getType() {
    return type;
  }
}