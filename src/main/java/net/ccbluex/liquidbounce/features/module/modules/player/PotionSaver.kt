/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.minecraft.network.play.client.C03PacketPlayer

object PotionSaver : Module("PotionSaver", Category.PLAYER, hideModule = false) {

    @EventTarget
    fun onPacket(e: PacketEvent) {
        val packet = e.packet

        if (packet is C03PacketPlayer && player?.isUsingItem == false && !packet.rotating &&
            (!packet.isMoving || (packet.x == player.lastTickPosX && packet.y == player.lastTickPosY && packet.z == player.lastTickPosZ)))
            e.cancelEvent()
    }

}