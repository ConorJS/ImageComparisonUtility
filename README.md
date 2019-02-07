# ImageComparisonUtility
A tool that finds similar pairs of images within a set of images


# Running the program
In IntelliJ, create a Maven configuration called something like 'JAR with dependencies'.
Set the working directory to the root directory of the repository, and set the run arguments to:
    clean compile package assembly:single

This will generate two JARs in %PROJECTROOT%\target, the larger file
(...jar-with-dependencies.jar) can run as a standalone application.