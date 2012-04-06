Code generators for Swagger compliant API's.

## Requirements

Install swagger-codegen fork:

    export SCALA_HOME=/usr/local/Cellar/scala/2.9.1/libexec
    hub mojolly/swagger-codegen
    cd swagger-codegen
    ant -f install-ivy
    ant deploy

## Generators

### Java

    sbt 'run-main mojolly.swagger.JavaLibCodeGen http://localhost:8080/1/swagger/ apikey backchat.client ../backchat-java-client api.backchat.io 80 /1 Backchat'

### Scala

    sbt 'run-main mojolly.swagger.ScalaLibCodeGen http://localhost:8080/1/swagger/ apikey backchat.client ../backchat-scala-client api.backchat.io 80 /1 Backchat'