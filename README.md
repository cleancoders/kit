#  c3kit : Clean Coders Clojure Kit

... libraries to help build clojure applications.

## [Apron](https://github.com/cleancoders/c3kit-apron) 

[![Apron](https://github.com/cleancoders/c3kit/blob/master/img/apron_200.png?raw=true)](https://github.com/cleancoders/c3kit-apron)

## [Scaffold](https://github.com/cleancoders/c3kit-scaffold)

[![Scaffold](https://github.com/cleancoders/c3kit/blob/master/img/scaffold_200.png?raw=true)](https://github.com/cleancoders/c3kit-scaffold)

## [Bucket](https://github.com/cleancoders/c3kit-bucket)

[![Bucket](https://github.com/cleancoders/c3kit/blob/master/img/bucket_200.png?raw=true)](https://github.com/cleancoders/c3kit-bucket)

## [Wire](https://github.com/cleancoders/c3kit-wire)

[![Wire](https://github.com/cleancoders/c3kit/blob/master/img/wire_200.png?raw=true)](https://github.com/cleancoders/c3kit-wire)

# Development

## Requirements
 
### Clojure

    brew install clojure/tools/clojure

## Structure

Each library is a [git submodule](https://git-scm.com/book/en/v2/Git-Tools-Submodules).  On initial checkout you'll need to run these commands:

    git submodule init
    git submodule update

There are script inside the `bin` directory to help with all the modules at the same time, such as tagging or committing.

## Deployment

c3kit had been using [Clojars](https://clojars.org/com.cleancoders.c3kit/apron) for deployment.  With the switch to the 
[Clojure CLI](https://clojure.org/guides/deps_and_cli), it's possible to use git repos as a dependency source.  Until
Clojars deployments are added, clients of c3kit should use the Clojure CLI instead of Leiningen.

Deployments are made by simply pushing to the Github repos.  Clients choose the SHA they want.  Significant updates
should be tagged with new version identifiers for progress reference.
