package dev.virefire.viira.types

class Headers : MutableMap<String, String> {
    val map = HashMap<String, String>()

    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = map.entries

    override val keys: MutableSet<String>
        get() = map.keys

    override val size: Int
        get() = map.size

    override val values: MutableCollection<String>
        get() = map.values

    override fun containsKey(key: String): Boolean {
        return map.containsKey(key.lowercase())
    }

    override fun containsValue(value: String): Boolean {
        return map.containsValue(value)
    }

    override fun get(key: String): String? {
        return map[key.lowercase()]
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun clear() {
        map.clear()
    }

    override fun putAll(from: Map<out String, String>) {
        map.putAll(from.mapKeys { it.key.lowercase() })
    }

    override fun remove(key: String): String? {
        return map.remove(key.lowercase())
    }

    override fun put(key: String, value: String): String? {
        return map.put(key.lowercase(), value)
    }
}