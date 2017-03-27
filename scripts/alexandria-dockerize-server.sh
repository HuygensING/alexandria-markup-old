mvn package && \
  export version=$(xmllint --xpath "/*[local-name()='project']/*[local-name()='version']/text()" pom.xml) && \
  cd alexandria-markup-xml-server && \
    docker build -t huygensing/alexandria-markup-xml-server -t huygensing/alexandria-markup-xml-server:$version --build-arg version=$version .
