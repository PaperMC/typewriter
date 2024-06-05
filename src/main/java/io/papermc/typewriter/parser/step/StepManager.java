package io.papermc.typewriter.parser.step;

import io.papermc.typewriter.parser.LineParser;
import io.papermc.typewriter.parser.StringReader;
import org.checkerframework.checker.nullness.qual.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;

public class StepManager {

    private final LineParser parser;
    private final Deque<IterativeStep> steps = new ArrayDeque<>(10);

    public StepManager(LineParser parser) {
        this.parser = parser;
    }

    public void enqueue(StepHolder holder) {
        for (IterativeStep step : holder.initialSteps()) {
            if (!this.steps.offerLast(step)) {
                throw new IllegalStateException("Cannot add a step into the queue!");
            }
        }
    }

    public void addPriority(IterativeStep step) {
        if (!this.steps.offerFirst(step)) {
            throw new IllegalStateException("Cannot add a priority step into the queue!");
        }
    }

    public void runSteps(StringReader line) {
        @Nullable IterativeStep step;
        while ((step = this.steps.peek()) != null) {
            if (!step.run(line, this.parser)) {
                this.steps.remove(step);
                // remove the current step not the last one because the current step can add other priority steps
            }

            if (!line.canRead()) {
                break;
            }
        }
    }

    public void clearRemaining() {
        this.steps.clear();
    }
}
