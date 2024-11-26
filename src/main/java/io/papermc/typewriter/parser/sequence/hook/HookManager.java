package io.papermc.typewriter.parser.sequence.hook;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class HookManager implements Hooks {

    private final Map<HookType, Callback> events = new EnumMap<>(HookType.class);

    @Override
    public void bind(HookType type, UnaryOperator<Hook> callback) {
        this.events.put(type, ((AbstractHook) callback.apply(type.emptyHook())).callback());
    }

    public void finishCallback() {
        for (HookType type : HookType.values()) {
            this.events.putIfAbsent(type, Callback.NO_OP);
        }
    }

    public void fire(HookType type, Consumer<Callback> callback) {
        callback.accept(this.events.get(type));
    }
}
