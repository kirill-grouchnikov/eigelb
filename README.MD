# eigelb

This is a Compose Multiplatform port of Paul Schmidinger's Java applet effect demos from https://eigelb.at/. 
Live web demos can be accessed at https://pushing-pixels.org/demos/eigelb/. This port is available under
the terms of the Apache license.

### Desktop
* Run the desktop application for Glitzer: `./gradlew :desktopApp:run` 
* Other demos (Grappa, Sparky) have their `main()` entrypoints under `desktopApp/src/main/kotlin`

### Web Distribution
* Build web distribution: `./gradlew :wasmJsBrowserDistribution`  
* Run the browser application locally: `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
* Deploy the `webApp/build/dist/wasmJs/productionExecutable` directory to a web server

