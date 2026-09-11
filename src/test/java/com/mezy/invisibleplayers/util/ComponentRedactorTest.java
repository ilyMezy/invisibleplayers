package com.mezy.invisibleplayers.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ComponentRedactorTest {

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void replacesKillerArgumentInTranslatableMessage() {
        // Mirrors the shape of a real vanilla "death.attack.player" message:
        // a translatable component with [victim, killer] as arguments.
        Component message = Component.translatable(
                "death.attack.player",
                Component.text("Steve"),
                Component.text("Alex")
        );

        Component result = ComponentRedactor.redactNames(message, Set.of("Alex"), Component.text("?"));

        assertEquals("Steve was slain by ?", legacyRender(result));
    }

    @Test
    void preservesVictimAndWeaponWhenRedactingKiller() {
        Component message = Component.translatable(
                "death.attack.arrow.item",
                Component.text("Steve"),
                Component.text("Alex"),
                Component.text("Bow")
        );

        Component result = ComponentRedactor.redactNames(message, Set.of("Alex"), Component.text("?"));

        TranslatableComponent redacted = (TranslatableComponent) result;
        assertEquals("Steve", plain(redacted.arguments().get(0).asComponent()));
        assertEquals("?", plain(redacted.arguments().get(1).asComponent()));
        assertEquals("Bow", plain(redacted.arguments().get(2).asComponent()));
    }

    @Test
    void leavesMessageUnchangedWhenKillerNameDoesNotMatch() {
        Component message = Component.translatable(
                "death.attack.player",
                Component.text("Steve"),
                Component.text("Notch")
        );

        Component result = ComponentRedactor.redactNames(message, Set.of("Alex"), Component.text("?"));

        assertEquals("Steve was slain by Notch", legacyRender(result));
    }

    @Test
    void discardsHoverAndClickEventsOnRedactedSubtree() {
        Component killerName = Component.text("Alex")
                .hoverEvent(HoverEvent.showText(Component.text("secret uuid or identity info")));
        Component message = Component.translatable(
                "death.attack.player",
                Component.text("Steve"),
                killerName
        );

        Component result = ComponentRedactor.redactNames(message, Set.of("Alex"), Component.text("?"));

        TranslatableComponent redacted = (TranslatableComponent) result;
        Component replacedArg = redacted.arguments().get(1).asComponent();
        assertNull(replacedArg.hoverEvent());
        assertFalse(plain(result).contains("secret"));
    }

    @Test
    void emptyNameSetReturnsInputUnchanged() {
        Component message = Component.text("Steve was slain by Alex");

        Component result = ComponentRedactor.redactNames(message, Set.of(), Component.text("?"));

        assertSame(message, result);
    }

    private static String legacyRender(Component component) {
        // Vanilla death messages don't resolve translation keys client-side without
        // a registry, so for this test we manually stitch the known key's shape.
        if (component instanceof TranslatableComponent translatable) {
            StringBuilder sb = new StringBuilder();
            var args = translatable.arguments();
            sb.append(plain(args.get(0).asComponent()));
            sb.append(" was slain by ");
            sb.append(plain(args.get(1).asComponent()));
            return sb.toString();
        }
        return plain(component);
    }
}
