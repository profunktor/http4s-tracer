http4s-tracer
=============

[![Actions Status](https://github.com/asachdeva/http4s-tracer/workflows/Build/badge.svg)](https://github.com/asachdeva/http4s-tracer/actions)
[![Gitter Chat](https://badges.gitter.im/profunktor-dev/http4s-tracer.svg)](https://gitter.im/profunktor-dev/http4s-tracer)
[![Maven Central](https://img.shields.io/maven-central/v/dev.profunktor/http4s-tracer_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Chttp4s-tracer) <a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>
[![MergifyStatus](https://img.shields.io/endpoint.svg?url=https://gh.mergify.io/badges/profunktor/http4s-tracer&style=flat)](https://mergify.io)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-brightgreen.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

End-to-end tracing system for `http4s`.

### Dependencies

Add this to your `build.sbt`:

```
libraryDependencies += "dev.profunktor" %% "http4s-tracer" % Version
```

If you would like to have [log4cats](https://christopherdavenport.github.io/log4cats/) support add this extra dependency:

```
libraryDependencies += "dev.profunktor" %% "http4s-tracer-log4cats" % Version
```

Note: previous versions `<= 1.2.1` were published using the `com.github.gvolpe` group id (see [migration
guide](https://github.com/profunktor/http4s-tracer/wiki/Migration-guide-(vim))).


Find out more in the [microsite](https://http4s-tracer.profunktor.dev/).

### Code of Conduct

See the [Code of Conduct](https://http4s-tracer.profunktor.dev/CODE_OF_CONDUCT)

## LICENSE

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this project except in compliance with
the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
language governing permissions and limitations under the License.

