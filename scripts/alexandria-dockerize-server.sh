mvn package && \
  export version=$(xmllint --xpath "/*[local-name()='project']/*[local-name()='version']/text()" pom.xml) && \
  cd alexandria-markup-server && \
    docker build -t huygensing/alexandria-markup-server -t huygensing/alexandria-markup-server:$version --build-arg version=$version .
