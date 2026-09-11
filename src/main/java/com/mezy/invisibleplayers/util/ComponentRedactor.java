package com.mezy.invisibleplayers.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Replaces any component subtree whose flattened plain text exactly matches one
 * of the given names with a fresh, plain replacement component.
 *
 * <p>Because a matching subtree is replaced wholesale, any hover event, click
 * event, or nested translatable structure attached to it is discarded along
 * with it — the replacement never leaks the original identity through any of
 * those channels. This deliberately avoids naive string substitution on a
 * rendered/legacy message, which could redact unrelated substrings.</p>
 */
public final class ComponentRedactor {

    private ComponentRedactor() {
    }

    public static Component redactNames(Component input, Set<String> namesToRedact, Component replacement) {
        if (namesToRedact.isEmpty()) {
            return input;
        }
        return redact(input, namesToRedact, replacement);
    }

    private static Component redact(Component input, Set<String> namesToRedact, Component replacement) {
        String plain = PlainTextComponentSerializer.plainText().serialize(input);
        if (!plain.isBlank() && namesToRedact.contains(plain)) {
            return replacement;
        }

        Component rebuilt = input;

        if (input instanceof TranslatableComponent translatable) {
            List<TranslationArgument> newArgs = new ArrayList<>(translatable.arguments().size());
            for (TranslationArgument arg : translatable.arguments()) {
                newArgs.add(redactArgument(arg, namesToRedact, replacement));
            }
            rebuilt = translatable.arguments(newArgs);
        }

        if (!rebuilt.children().isEmpty()) {
            List<Component> newChildren = new ArrayList<>(rebuilt.children().size());
            for (Component child : rebuilt.children()) {
                newChildren.add(redact(child, namesToRedact, replacement));
            }
            rebuilt = rebuilt.children(newChildren);
        }

        return rebuilt;
    }

    private static TranslationArgument redactArgument(TranslationArgument arg, Set<String> namesToRedact, Component replacement) {
        if (arg.value() instanceof Component componentValue) {
            Component redacted = redact(componentValue, namesToRedact, replacement);
            if (!redacted.equals(componentValue)) {
                return TranslationArgument.component(redacted);
            }
        }
        return arg;
    }
}
