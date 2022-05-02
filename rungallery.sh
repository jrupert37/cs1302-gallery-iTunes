#!/usr/bin/bash -ex

mvn clean
mvn compile
mvn -e -Dprism.order=sw exec:java
