Code generators for Swagger compliant api's.

## Generators

### Scala

    sbt 'run-main mojolly.swagger.ScalaLibCodeGen http://localhost:8080/1/swagger/ foobar foo.bar generated'

### Java

    sbt 'run-main mojolly.swagger.JavaLibCodeGen http://localhost:8080/1/swagger/ foobar foo.bar generated'