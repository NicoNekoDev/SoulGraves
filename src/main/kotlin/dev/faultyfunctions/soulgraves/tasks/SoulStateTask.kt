package dev.faultyfunctions.soulgraves.tasks

import dev.faultyfunctions.soulgraves.SoulGraves
import dev.faultyfunctions.soulgraves.managers.ConfigManager
import dev.faultyfunctions.soulgraves.soulTimeLeftKey
import dev.faultyfunctions.soulgraves.utils.SoulState
import com.jeff_media.morepersistentdatatypes.DataType
import org.bukkit.Bukkit
import org.bukkit.entity.Marker
import org.bukkit.scheduler.BukkitRunnable

class SoulStateTask : BukkitRunnable() {
	override fun run() {
		for (soul in SoulGraves.soulList) {
			if (ConfigManager.offlineOwnerTimerFreeze && Bukkit.getPlayer(soul.ownerUUID) == null) { continue }

			// SET STATES
			if (soul.timeLeft > ConfigManager.timeUnstable) {
				soul.state = SoulState.NORMAL
			} else if (soul.timeLeft <= ConfigManager.timeUnstable && soul.timeLeft > 0) {
				soul.state = SoulState.PANIC
			} else {
				soul.state = SoulState.EXPLODING
			}

			soul.timeLeft -= 1

			// LOAD CHUNK & GRAB ENTITY
			soul.location.world?.loadChunk(soul.location.chunk)
			val soulEntity: Marker = Bukkit.getEntity(soul.markerUUID) as Marker

			// STORE TIME LEFT IN ENTITY'S PDC
			soulEntity.persistentDataContainer.set(soulTimeLeftKey, DataType.INTEGER, soul.timeLeft)

			// UNLOAD CHUNK
			soul.location.world?.unloadChunk(soul.location.chunk)
		}
	}
}