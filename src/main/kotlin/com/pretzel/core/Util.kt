package com.pretzel.core

import java.lang.reflect.Modifier

object Util {
    fun serializeObjectDict(obj: Any?): String {
        if (obj == null) {
            return "(null)"
        }

        if (obj is Iterable<*>) {
            var counter = 0
            return "[" + obj.joinToString { "${counter++}: $it, " } + "]"
        }

        val fldDict = StringBuilder("{")
        for (fld in obj.javaClass.fields) {
            if (Modifier.isPublic(fld.modifiers)) {
                fldDict.append("${fld.name}: ${fld.get(obj)}, ")
            }
        }

        return fldDict.substring(0, fldDict.length - 2) + "}"
    }
}

fun <T> Boolean.doIf(ifTrue: () -> T, ifFalse: () -> T): T {
    return if (this) ifTrue() else ifFalse()
}

fun <T> T.otherIs(a: T, b: T): Boolean {
    return (a == this || b == this) && (a != b)
}

fun <T> T.toList(): List<T> {
    return listOf(this)
}

fun String.ensureEnd(end: String): String {
    return if (!this.endsWith(end)) {
        this + end
    } else {
        this
    }
}