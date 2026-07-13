javac -cp "lib/*" -d out mg/itu/**/*.java mg/itu/*.java
jar cvf framework.jar -C out/ .