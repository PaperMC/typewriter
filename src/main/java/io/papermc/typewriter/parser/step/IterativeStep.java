package io.papermc.typewriter.parser.step;

import io.papermc.typewriter.parser.LineParser;
import io.papermc.typewriter.parser.StringReader;

public sealed interface IterativeStep permits IterativeStep.Once, IterativeStep.Repeat {

    boolean run(StringReader line, LineParser parser); // true to repeat the step

    @FunctionalInterface
    interface Runner {

        void run(StringReader line, LineParser parser);
    }

    @FunctionalInterface
    interface RepeatRunner {

        boolean repeatUntil(StringReader line, LineParser parser);
    }

    final class Once implements IterativeStep {

        private final Runner runner;

        public Once(Runner runner) {
            this.runner = runner;
        }

        @Override
        public boolean run(StringReader line, LineParser parser) {
            this.runner.run(line, parser);
            return false;
        }
    }

    final class Repeat implements IterativeStep {

        private final RepeatRunner runner;

        public Repeat(RepeatRunner runner) {
            this.runner = runner;
        }

        @Override
        public boolean run(StringReader line, LineParser parser) {
            return this.runner.repeatUntil(line, parser);
        }
    }
}
