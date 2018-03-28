package it.softsolutions.bestx;

import java.util.concurrent.Executor;

public class TrivialExecutor implements Executor {
    public void execute(Runnable command) {
        command.run();
    }
}
