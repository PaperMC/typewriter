package io.papermc.typewriter.parser.step;

import org.jetbrains.annotations.Contract;

public interface StepHolder {

    IterativeStep[] initialSteps();

    @Contract(value = "_ -> new", pure = true)
    default IterativeStep onceStep(IterativeStep.Runner runner) {
        return new IterativeStep.Once(runner);
    }

    @Contract(value = "_ -> new", pure = true)
    default IterativeStep repeatStep(IterativeStep.RepeatRunner runner) {
        return new IterativeStep.Repeat(runner);
    }
}
