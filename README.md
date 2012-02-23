Code generators for Swagger compliant api's.

## Requirements

Install swagger-codegen fork:

    export SCALA_HOME=/usr/local/Cellar/scala/2.9.1/libexec
    hub mojolly/swagger-codegen
    cd swagger-codegen
    ant -f install-ivy
    ant deploy

## Generators

### Scala

    sbt 'run-main mojolly.swagger.ScalaLibCodeGen http://localhost:8080/1/swagger/ <api_key> foobar.client ../foobar-scala-client api.foobar.com 80 /1 Foobar'