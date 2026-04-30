package com.aetherbound.game.core.data

import com.aetherbound.game.core.EchoformInstance

/**
 * Player's active party — max 6 Echoforms, like the Pokémon convention.
 * Anything caught beyond 6 goes to [PcStorage].
 *
 * Immutable: every mutation returns a new [Party] copy. This makes
 * undo/redo trivial and matches Compose's snapshot model.
 */
data class Party(
    val members: List<EchoformInstance> = emptyList(),
    val activeIndex: Int = 0,
) {
    init {
        require(members.size <= MAX) { "party can hold at most $MAX members, got ${members.size}" }
        require(activeIndex == 0 || activeIndex in members.indices) {
            "activeIndex out of range: $activeIndex (members=${members.size})"
        }
    }

    val active: EchoformInstance? get() = members.getOrNull(activeIndex)
    val isEmpty: Boolean get() = members.isEmpty()
    val isFull: Boolean get() = members.size >= MAX
    val aliveCount: Int get() = members.count { !it.isFainted }
    val isWipedOut: Boolean get() = members.isNotEmpty() && aliveCount == 0

    fun add(instance: EchoformInstance): AddResult {
        return if (members.size < MAX) AddResult.Added(copy(members = members + instance))
        else AddResult.PartyFull(instance)
    }

    fun replace(index: Int, instance: EchoformInstance): Party {
        val newMembers = members.toMutableList()
        newMembers[index] = instance
        return copy(members = newMembers)
    }

    fun remove(index: Int): Party {
        if (index !in members.indices) return this
        val newMembers = members.toMutableList()
        newMembers.removeAt(index)
        val newActive = when {
            newMembers.isEmpty() -> 0
            activeIndex == index -> 0
            activeIndex > index -> activeIndex - 1
            else -> activeIndex
        }
        return copy(members = newMembers, activeIndex = newActive)
    }

    fun switch(toIndex: Int): Party {
        if (toIndex !in members.indices) return this
        if (members[toIndex].isFainted) return this  // cannot switch to fainted
        return copy(activeIndex = toIndex)
    }

    fun firstAliveIndex(): Int? = members.indexOfFirst { !it.isFainted }.takeIf { it >= 0 }

    /**
     * Return a Party with the active member auto-switched to the first non-fainted
     * member if the current active is down. Returns the same party if nothing
     * needs switching, or a wiped-out party if no replacements exist.
     */
    fun ensureAliveActive(): Party {
        val a = active ?: return this
        if (!a.isFainted) return this
        val next = firstAliveIndex() ?: return this  // wipe
        return copy(activeIndex = next)
    }

    sealed class AddResult {
        data class Added(val party: Party) : AddResult()
        data class PartyFull(val overflow: EchoformInstance) : AddResult()
    }

    companion object {
        const val MAX = 6
    }
}

/**
 * Overflow storage for Echoforms beyond the 6-party slot. Pokémon's "PC".
 * Implemented as a flat list with optional named boxes (30 each, like PC).
 */
data class PcStorage(
    val boxes: List<PcBox> = listOf(PcBox("Box 1")),
) {
    fun deposit(instance: EchoformInstance): PcStorage {
        // Insert into the first box with capacity; else create a new box.
        val mutable = boxes.toMutableList()
        for (i in mutable.indices) {
            if (mutable[i].slots.size < PcBox.CAPACITY) {
                mutable[i] = mutable[i].copy(slots = mutable[i].slots + instance)
                return copy(boxes = mutable)
            }
        }
        mutable += PcBox("Box ${mutable.size + 1}", listOf(instance))
        return copy(boxes = mutable)
    }

    val totalCount: Int get() = boxes.sumOf { it.slots.size }
}

data class PcBox(
    val name: String,
    val slots: List<EchoformInstance> = emptyList(),
) {
    companion object { const val CAPACITY = 30 }
}
