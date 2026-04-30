package com.aetherbound.game.core.data

/**
 * Player inventory. Stack-counts by item slug. Categories mirror Tuxemon's
 * `sort` field so the UI can group items into Bag pages (Heal / Balls /
 * Berries / Key / etc).
 *
 * Immutable: mutations return a new Inventory.
 */
data class Inventory(
    val stacks: Map<String, Int> = emptyMap(),
    val money: Int = 0,
) {
    fun count(slug: String): Int = stacks[slug] ?: 0
    fun has(slug: String, n: Int = 1): Boolean = count(slug) >= n
    val totalItems: Int get() = stacks.values.sum()
    val isEmpty: Boolean get() = stacks.isEmpty()

    /** Add [amount] of [slug]. Returns new inventory with the larger stack. */
    fun add(slug: String, amount: Int = 1): Inventory {
        if (amount <= 0) return this
        val newCount = (count(slug) + amount).coerceAtMost(MAX_STACK)
        return copy(stacks = stacks + (slug to newCount))
    }

    /** Remove [amount] of [slug]. Returns null if the inventory has fewer than [amount]. */
    fun remove(slug: String, amount: Int = 1): Inventory? {
        if (amount <= 0) return this
        val current = count(slug)
        if (current < amount) return null
        val remaining = current - amount
        return if (remaining == 0) copy(stacks = stacks - slug)
        else copy(stacks = stacks + (slug to remaining))
    }

    /** Add money (e.g. trainer-battle reward). */
    fun earn(amount: Int): Inventory = copy(money = (money + amount).coerceAtLeast(0))

    /** Spend money. Returns null if insufficient funds. */
    fun spend(amount: Int): Inventory? {
        if (amount > money) return null
        return copy(money = money - amount)
    }

    /** Items grouped by Tuxemon `sort` (passed through a [TuxemonItem] lookup). */
    fun groupedBySort(itemLookup: (String) -> TuxemonItem?): Map<String, List<Pair<String, Int>>> {
        val out = mutableMapOf<String, MutableList<Pair<String, Int>>>()
        for ((slug, n) in stacks) {
            val sort = itemLookup(slug)?.sort ?: "misc"
            out.getOrPut(sort) { mutableListOf() } += slug to n
        }
        return out
    }

    companion object {
        const val MAX_STACK = 99
    }
}
