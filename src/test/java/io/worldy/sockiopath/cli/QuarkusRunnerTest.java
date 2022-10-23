package io.worldy.sockiopath.cli;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

@QuarkusMainTest
public class QuarkusRunnerTest {
    @Test
    public void constructorTest() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        QuarkusRunner.class.getConstructor().newInstance();
    }

    @Test
    @Launch({"-s", "false", "-c", "false"})
    public void testLaunchCommand(LaunchResult result) {
        Assertions.assertTrue(result.getOutput().contains("No server or client was started. Closing CLI."));
    }
}
