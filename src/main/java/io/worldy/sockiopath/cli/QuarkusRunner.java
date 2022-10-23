package io.worldy.sockiopath.cli;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class QuarkusRunner {

    public static void main(String... args) {
        Quarkus.run(MyQuarkusApp.class, args);
    }

    static class MyQuarkusApp implements QuarkusApplication {
        @Override
        public int run(String... args) throws Exception {
            SockiopathCommandLine.main(args);
            return 0;
        }
    }
}
