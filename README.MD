# eigelb

This is a Compose Multiplatform port of Paul Schmidinger's Java applet effect demos from https://eigelb.at/. 
Live web demos can be accessed at https://pushing-pixels.org/demos/eigelb/.

### Desktop
* Run the desktop application for Glitzer: `./gradlew :desktopApp:run`  

### Web Distribution
* Build web distribution: `./gradlew :wasmJsBrowserDistribution`  
* Run the browser application locally: `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
* Deploy the `webApp/build/dist/wasmJs/productionExecutable` directory to a web server

