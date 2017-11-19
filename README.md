# Cameleon - Clojure(Script) utility library for file and HTTP I/O, HTTP mocking, and URI matching

***
__WARNING: EXPERIMENTAL LIBRARY - SUBJECT TO CHANGE - USE AT YOUR OWN RISK__
***

Cameleon is a utility library to support writing programs than can run on the [JVM](https://en.wikipedia.org/wiki/Java_virtual_machine) or on a [JavaScript](https://en.wikipedia.org/wiki/JavaScript) runtime such as [NodeJS](https://en.wikipedia.org/wiki/Node.js).

## Rationale

Writing programs that can run on both the JVM or in JS could prove useful for:

- __Increased capabilities__: both JVM and JS ecosystems are very rich, but not everything in one can be found in the other, therefore why limiting oneself to one platform? The JVM platform provides a plethora of libraries that increase integration capabilities. The Javascript environment benefits from a shorter startup time which helps when a program needs to be part of a [Continuous Integration](https://en.wikipedia.org/wiki/Continuous_integration) step.
- __Increased reach__: both ecosystems have a very wide user base and reaching both communities can provide a significant advantage.
- __Delay the choice between JVM and JS__: you may not be ready yet to commit to just one platform.

Although there are a few libraries that support both platforms, the coding experience is often not ideal and may require the use of many [reader conditionals](https://clojure.org/guides/reader_conditionals). This is particularly the case when:

- Calling HTTP REST services, depending on the platform you need to select and use different libraries to support:
  - HTTP client calls
  - JSON serialisation and deserialisation
- Testing is another area where it may be cumbersome to write truly portable code, especially around:
  - writing temporary files
  - using test fixture files
  - mocking HTTP services

## Usage
Require and use the various utility functions which are spread across several namespaces:

- `cameleon.io`: file I/O
- `cameleon.http`: calling HTTP end-points
- `cameleon.rest`: calling HTTP REST end-points
- `cameleon.uri`: parsing URI/URLs
- `cameleon.uri-matcher`: matching/routing on full URI/URL (including hostname)
- `cameleon.http-client-mock`: mocking HTTP or REST end-points without needing any network stack and using instead custom HTTP client instances over fixtures files which can be recorded from actual traffic
- `cameleon.repo`: extract repo information from a (github) repo URI/URL, this may be an odd one here, but it's a good `cameleon.uri-matcher` example, and it could prove useful in a CI context.

For more details and examples see the corresponding test files.

![Cameleon namespaces dependency graph](ns-dep-graph.png?raw=true "Cameleon namespaces dependency graph")

## Instructions for developers

This tool has been coded in Clojure(Script) with both Javascript and JVM as compilation targets. Therefore the majority of the code as well as test cases are written in `.cljc` files which are clojure files that can contain platform specific directives.

### Testing
The unit tests are identical for both JVM and NodeJS, but each platform has its own command line. To test on the JVM:

    $ lein test

To test on the node JS platform:
    
    $ lein cljsbuild test

Or use the alias shorthand

    $ lein cljstest
    
    
Tests requiring calls to HTTP end-points can use fixture files to avoid relying on the network or availability of said endpoints. These are found under the `test/fixtures` directory. To regenerate a fixture file just delete it and run the corresponding test when network and end-points are available.

Also in ClojureScript new test namespaces are not automatically added to the project test suite, they must be manually added to `test/cljs/cameleon/test_runner.cljs`

## License
Eclipse Public License - EPL 1.0

[http://www.eclipse.org/legal/epl-v10.html](http://www.eclipse.org/legal/epl-v10.html)

Copyright © 2017 François Rey
