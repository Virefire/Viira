package dev.virefire.viira.types

import dev.virefire.viira.exceptions.PathParsingException
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors

open class RouteEntry {
    companion object {
        fun parse(path: String): List<RouteEntry> {
            val entries: ArrayList<RouteEntry> =
                ArrayList<RouteEntry>()
            for (entry in path.split("/").toTypedArray()) {
                if (entry.contains(":")) {
                    val parts = entry.split(":").toTypedArray()
                    if (parts.size != 2) throw PathParsingException("Invalid path param segment: $path($entry)")
                    entries.add(Param(parts[0], parts[1]))
                } else if (entry.contains("*") || entry.contains("?")) {
                    entries.add(Wildcard(entry))
                } else {
                    if (entry != "") entries.add(Literal(entry))
                }
            }
            return entries
        }

        fun check(entries: List<RouteEntry>, path: String): CheckResult {
            val pathParts = Arrays.stream(path.split("/").toTypedArray()).filter { e: String -> e != "" }
                .collect(Collectors.toList())
            var match = true
            var exactMatch = true
            val params = HashMap<String, String>()
            val wildcard = ArrayList<String>()
            if (entries.size < pathParts.size) {
                exactMatch = false
            } else if (entries.size > pathParts.size) {
                return CheckResult(match = false, exact = false, params = params, wildcard = wildcard)
            }
            for (i in entries.indices) {
                val entry: RouteEntry = entries[i]
                val pathPart = pathParts[i]
                if (entry is Literal) {
                    val literal = entry
                    if (literal.literal != pathPart) match = false
                } else if (entry is Param) {
                    val param = entry
                    if (!pathPart.startsWith(param.prefix)) {
                        match = false
                        continue
                    }
                    val value: String = pathPart.substring(param.prefix.length)
                    params[param.name] = value
                } else if (entry is Wildcard) {
                    val wildcardEntry = entry
                    var current = 0
                    for (j in 0 until wildcardEntry.wildcard.size) {
                        val w: String = wildcardEntry.wildcard[j]
                        if (w == "*") {
                            val next: String? =
                                (if (wildcardEntry.wildcard.size > j + 1) wildcardEntry.wildcard[j + 1] else null)
                            if (next != null) {
                                var found = false
                                for (k in current until pathPart.length) {
                                    val p = pathPart.substring(k)
                                    if (p.startsWith(next)) {
                                        wildcard.add(pathPart.substring(current, k))
                                        current += k
                                        found = true
                                        break
                                    }
                                }
                                if (!found) match = false
                            } else {
                                wildcard.add(pathPart.substring(current))
                            }
                        } else if (w == "?") {
                            if (pathPart.length > current) {
                                wildcard.add(pathPart.substring(current, current + 1))
                                current++
                            } else match = false
                        } else {
                            if (pathPart.substring(current).startsWith(w)) {
                                current += w.length
                            } else match = false
                        }
                    }
                    if (current != pathPart.length) match = false
                }
            }
            if (!match) {
                exactMatch = false
                params.clear()
                wildcard.clear()
            }
            return CheckResult(match, exactMatch, params, wildcard)
        }
    }

    class Literal(
        val literal: String
    ) : RouteEntry()

    class Param(
        val prefix: String,
        val name: String
    ) : RouteEntry()

    class Wildcard(wildcard: String) : RouteEntry() {
        val wildcard: MutableList<String> = ArrayList()

        init {
            if (wildcard.contains("**") || wildcard.contains("?*") || wildcard.contains("*?")) throw PathParsingException(
                "Wildcard must not contain **, ?* or *? constructions: $wildcard"
            )
            val m = Pattern.compile("[*?]").matcher(wildcard)
            var start = 0
            while (m.find()) {
                if (start != m.start()) this.wildcard.add(wildcard.substring(start + 1, m.start()))
                this.wildcard.add(m.group())
                start = m.start()
            }
            if (start < wildcard.length - 1) this.wildcard.add(wildcard.substring(start + 1))
        }
    }

    class CheckResult(
        val match: Boolean = false,
        val exact: Boolean = false,
        val params: Map<String, String>,
        val wildcard: List<String>,
    )
}