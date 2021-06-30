package com.pretzel.core.parser.ast.annot

import com.pretzel.core.otherIs

enum class Modifier(val value: Int, val type: ModifierType) {
    // Note: these are not the same as the PVM modifier integers.
    PUBLIC(1 shl 0, ModifierType.ACCESS),
    PRIVATE(1 shl 1, ModifierType.ACCESS),
    PROTECTED(1 shl 2, ModifierType.ACCESS),
    SHARED(1 shl 3, ModifierType.BINDING),
    FINAL(1 shl 4, ModifierType.WRITEABILITY);

    companion object {
        fun isCompatible(
            a: Modifier,
            b: Modifier,
            targetType: ModifierTargetType
        ): ModifierCompatibilityState {
            return if (a == b) {
                ModifierCompatibilityState.DUPLICATE
            } else if (ModifierType.ACCESS.otherIs(a.type, b.type)) {
                ModifierCompatibilityState.INCOMPATIBLE
            } else if (SHARED.otherIs(a, b) && FINAL.otherIs(a, b)) {
                if (targetType == ModifierTargetType.METHOD)
                    ModifierCompatibilityState.REDUNDANT
                else
                    ModifierCompatibilityState.COMPATIBLE
            } else if (a == PRIVATE && b == FINAL) {
                ModifierCompatibilityState.REDUNDANT
            } else {
                ModifierCompatibilityState.COMPATIBLE
            }
        }
    }
}

enum class ModifierType {
    ACCESS,
    BINDING,
    WRITEABILITY,
}

enum class ModifierTargetType {
    VARIABLE,
    METHOD,
}

enum class ModifierCompatibilityState {
    INCOMPATIBLE,
    COMPATIBLE,
    DUPLICATE,
    REDUNDANT,
}