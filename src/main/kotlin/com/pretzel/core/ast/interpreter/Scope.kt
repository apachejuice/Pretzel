/*
 * Copyright 2020 Valio Valtokari
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pretzel.core.ast.interpreter

class Scope(private var parent: Scope? = null) {
    private val vars: MutableMap<String, Any> = HashMap()
    private val children: MutableList<Scope> = ArrayList()

    fun createChild() = Scope(this)

    fun addChild(s: Scope) {
        s.parent = this
        children.add(s)
    }

    fun copy(s: Scope, childForThis: Boolean = false): Scope {
        val new = if (childForThis) createChild() else Scope()
        new.vars.putAll(s.vars)
        return s
    }

    operator fun get(id: String) = vars[id]

    init {
        if (parent != null) this.vars.putAll(parent!!.vars)
    }
}