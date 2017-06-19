[<img src="https://img.shields.io/travis/playframework/play-scala-starter-example.svg"/>](https://travis-ci.org/playframework/play-scala-starter-example)

# Play Scala Starter

This is a starter application that shows how Play works.  Please see the documentation at https://www.playframework.com/documentation/latest/Home for more details.

## Essential config Intranet
A someFile.conf is a must for the ldap security credentials (we aren't storing them in source control)

The production file should include: 
	include "application"
	play.crypto.secret="someSecret"

	ldap.admin_dn = "someAdminDN"
	ldap.admin_pass = "someAdminPass"

	# and any other config that you want to keep out of the default application.conf in VC.

To run the app with the production conf: productionFile.bat -Dconfig.file=C:/pathToProduction.conf

## Building for production Intranet
https://www.playframework.com/documentation/2.5.x/Production
Requires SBT(see scala-sbt link below)

cmd: sbt ~dist
IDE: Edit run configurations -> Create a new SBT task -> Tasks: dist
This creates a zip file in playPath/target/universal
Extract the zip file, and run the snapshot/bin/play-scala.bat file from any environment with a java jdk. 

## Building with angular Intranet
Current, Manual Build process (these are in the respective readme's. but placed here for convenience):

Angular- npm run build.prod
Play api- sbt ~dist

Move the generated folders, clientPath/dist/prod, into the client-angular folder.

Restart client in IIS.


## Running

Run this using [sbt](http://www.scala-sbt.org/).  If you downloaded this project from http://www.playframework.com/download then you'll find a prepackaged version of sbt in the project directory:

```
sbt run
```

And then go to http://localhost:9000 to see the running web application.

There are several demonstration files available in this template.

## Controllers

- HomeController.scala:

  Shows how to handle simple HTTP requests.

- AsyncController.scala:

  Shows how to do asynchronous programming when handling a request.

- CountController.scala:

  Shows how to inject a component into a controller and use the component when
  handling requests.

## Components

- Module.scala:

  Shows how to use Guice to bind all the components needed by your application.

- Counter.scala:

  An example of a component that contains state, in this case a simple counter.

- ApplicationTimer.scala:

  An example of a component that starts when the application starts and stops
  when the application stops.

## Filters

- Filters.scala:

  Creates the list of HTTP filters used by your application.

- ExampleFilter.scala

  A simple filter that adds a header to every response.
