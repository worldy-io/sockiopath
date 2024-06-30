# sockiopath
A UDP and WebSocket server


## Running Locally

### Maven
```shell script
./mvnw quarkus:dev -Dquarkus.args="-c=true -s=true" -Dquarkus.log.level=DEBUG
```
### Java Executable
#### Build
```shell script
./mvnw -B clean install --file pom.xml
```

#### Run client only
```shell script
java -cp target/sockiopath-0.0.14-SNAPSHOT-jar-with-dependencies.jar io.worldy.sockiopath.cli.SockiopathCommandLine
```

### Native Executable
#### Build
```shell script
./mvnw -B clean install -Dnative --file pom.xml
```

#### Run client only
```shell script
./target/sockiopath-0.0.14-SNAPSHOT-runner -c=true -s=false -Dquarkus.log.level=DEBUG
```

#### Install locally
##### Linux
```shell script
sudo update-alternatives --install /usr/bin/sockiopath sockiopath /target/sockiopath-0.0.14-SNAPSHOT-runner 10
```


## Local Development
Set up the pre-commit hook
```shell script
ln -s ../../pre-commit.sh .git/hooks/pre-commit
```









