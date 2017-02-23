destinationDir=$1
version=$(grep "<version>" pom.xml |head -1 |cut -d '>' -f 2|cut -d '<' -f 1)
mvn -pl alexandria-markup-server -am package
cp -v alexandria-markup-server/target/alexandria-markup-server-${version}.jar ${destinationDir}/alexandria-markup-server.jar

mvn -pl alexandria-markup-java-client -am package
cp -v alexandria-markup-java-client/target/alexandria-markup-java-client-${version}.jar ${destinationDir}/alexandria-markup-java-client.jar
