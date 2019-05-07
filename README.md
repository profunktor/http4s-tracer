http4s-tracer
=============

[![CircleCI](https://circleci.com/gh/profunktor/http4s-tracer.svg?style=svg)](https://circleci.com/gh/profunktor/http4s-tracer)
[![Gitter Chat](https://badges.gitter.im/profunktor-dev/http4s-tracer.svg)](https://gitter.im/profunktor-dev/http4s-tracer)
[![Maven Central](https://img.shields.io/maven-central/v/dev.profunktor/http4s-tracer_2.12.svg)](http://search.maven.org/#search%7Cga%7C1%7Chttp4s-tracer) <a href="https://typelevel.org/cats/"><img src="https://typelevel.org/cats/img/cats-badge.svg" height="40px" align="right" alt="Cats friendly" /></a>

Distributed end-to-end tracing system for `http4s`.

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

