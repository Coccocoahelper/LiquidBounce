/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.aac.*
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.blocksmc.BlocksMC
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.blocksmc.BlocksMC2
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.hypixel.BoostHypixel
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.hypixel.FreeHypixel
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.hypixel.Hypixel
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.ncp.NCP
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.ncp.OldNCP
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other.*
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.rewinside.Rewinside
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.rewinside.TeleportRewinside
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.spartan.BugSpartan
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.spartan.Spartan
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.spartan.Spartan2
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vanilla.SmoothVanilla
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vanilla.Vanilla
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vulcan.Vulcan
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vulcan.VulcanGhost
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vulcan.VulcanOld
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.stop
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawPlatform
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.AxisAlignedBB
import org.lwjgl.input.Keyboard
import java.awt.Color

object Fly : Module("Fly", ModuleCategory.MOVEMENT, Keyboard.KEY_F, hideModule = false) {
    private val flyModes = arrayOf(
        Vanilla, SmoothVanilla,

        // NCP
        NCP, OldNCP,

        // AAC
        AAC1910, AAC305, AAC316, AAC3312, AAC3312Glide, AAC3313,

        // CubeCraft
        CubeCraft,

        // Hypixel
        Hypixel, BoostHypixel, FreeHypixel,

        // Rewinside
        Rewinside, TeleportRewinside,

        // Other server specific flys
        Mineplex, NeruxVace, Minesucht, Redesky, BlocksMC, BlocksMC2,

        // Spartan
        Spartan, Spartan2, BugSpartan,

        // Vulcan
        Vulcan, VulcanOld, VulcanGhost,

        // Other anti-cheats
        MineSecure, HawkEye, HAC, WatchCat, Verus,

        // Other
        Jetpack, KeepAlive, Collide, Jump, Flag
    )

    private val modes = flyModes.map { it.modeName }.toTypedArray()

    val mode by ListValue("Mode", modes, "Vanilla")

    val vanillaSpeed by FloatValue("VanillaSpeed", 2f, 0f..10f, subjective = true)
        { mode in arrayOf("Vanilla", "KeepAlive", "MineSecure", "BugSpartan") }
    private val vanillaKickBypass by BoolValue("VanillaKickBypass", false, subjective = true)
        { mode in arrayOf("Vanilla", "SmoothVanilla") }
    val ncpMotion by FloatValue("NCPMotion", 0f, 0f..1f) { mode == "NCP" }

    // AAC
    val aacSpeed by FloatValue("AAC1.9.10-Speed", 0.3f, 0f..1f) { mode == "AAC1.9.10" }
    val aacFast by BoolValue("AAC3.0.5-Fast", true) { mode == "AAC3.0.5" }
    val aacMotion by FloatValue("AAC3.3.12-Motion", 10f, 0.1f..10f) { mode == "AAC3.3.12" }
    val aacMotion2 by FloatValue("AAC3.3.13-Motion", 10f, 0.1f..10f) { mode == "AAC3.3.13" }

    // Hypixel
    val hypixelBoost by BoolValue("Hypixel-Boost", true) { mode == "Hypixel" }
    val hypixelBoostDelay by IntegerValue("Hypixel-BoostDelay", 1200, 50..2000)
        { mode == "Hypixel" && hypixelBoost }
    val hypixelBoostTimer by FloatValue("Hypixel-BoostTimer", 1f, 0.1f..5f)
        { mode == "Hypixel" && hypixelBoost }

    // Other
    val mineplexSpeed by FloatValue("MineplexSpeed", 1f, 0.5f..10f) { mode == "Mineplex" }
    val neruxVaceTicks by IntegerValue("NeruxVace-Ticks", 6, 2..20) { mode == "NeruxVace" }
    val redeskyHeight by FloatValue("Redesky-Height", 4f, 1f..7f) { mode == "Redesky" }

    // Verus
    val damage by BoolValue("Damage", false) { mode == "Verus" }
    val timerSlow by BoolValue("TimerSlow", true) { mode == "Verus" }
    val boostTicksValue by IntegerValue("BoostTicks", 20, 1..30) { mode == "Verus" }
    val boostMotion by FloatValue("BoostMotion", 6.5f, 1f..9.85f) { mode == "Verus" }
    val yBoost by FloatValue("YBoost", 0.42f, 0f..10f) { mode == "Verus" }

    // BlocksMC
    val stable by BoolValue("Stable", false) { mode == "BlocksMC" || mode == "BlocksMC2" }
    val timerSlowed by BoolValue("TimerSlowed", true) { mode == "BlocksMC" || mode == "BlocksMC2" }
    val boostSpeed by FloatValue("BoostSpeed", 6f, 1f..15f) { mode == "BlocksMC" || mode == "BlocksMC2" }
    val extraBoost by FloatValue("ExtraSpeed", 1f, 0.0F..2f) { mode == "BlocksMC" || mode == "BlocksMC2" }
    val stopOnLanding by BoolValue("StopOnLanding", true) { mode == "BlocksMC" || mode == "BlocksMC2" }
    val stopOnNoMove by BoolValue("StopOnNoMove", false) { mode == "BlocksMC" || mode == "BlocksMC2" }
    val debugFly by BoolValue("Debug", false) { mode == "BlocksMC" || mode == "BlocksMC2" }

    // Visuals
    private val mark by BoolValue("Mark", true, subjective = true)

    var jumpY = 0.0

    var startY = 0.0
        private set

    private val groundTimer = MSTimer()
    private var wasFlying = false

    override fun onEnable() {
        val thePlayer = mc.thePlayer ?: return

        startY = thePlayer.posY
        jumpY = thePlayer.posY
        wasFlying = mc.thePlayer.capabilities.isFlying

        modeModule.onEnable()
    }

    override fun onDisable() {
        val thePlayer = mc.thePlayer ?: return

        if (!mode.startsWith("AAC") && mode != "Hypixel" && mode != "SmoothVanilla" && mode != "Vanilla" && mode != "Rewinside" && mode != "Collide" && mode != "Jump") {
            if (mode == "CubeCraft") thePlayer.stopXZ()
            else thePlayer.stop()
        }

        thePlayer.capabilities.isFlying = wasFlying
        mc.timer.timerSpeed = 1f
        thePlayer.speedInAir = 0.02f

        modeModule.onDisable()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        modeModule.onUpdate()
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (!mark || mode == "Vanilla" || mode == "SmoothVanilla")
            return

        val y = startY + 2.0 + (if (mode == "BoostHypixel") 0.42 else 0.0)
        drawPlatform(
            y,
            if (mc.thePlayer.entityBoundingBox.maxY < y) Color(0, 255, 0, 90) else Color(255, 0, 0, 90),
            1.0
        )

        modeModule.onRender3D(event)
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        mc.thePlayer ?: return

        modeModule.onPacket(event)
    }

    @EventTarget
    fun onBB(event: BlockBBEvent) {
        mc.thePlayer ?: return

        modeModule.onBB(event)
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        modeModule.onJump(event)
    }

    @EventTarget
    fun onStep(event: StepEvent) {
        modeModule.onStep(event)
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        modeModule.onMotion(event)
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        modeModule.onMove(event)
    }

    fun handleVanillaKickBypass() {
        if (!vanillaKickBypass || !groundTimer.hasTimePassed(1000)) return
        val ground = calculateGround() + 0.5
        run {
            var posY = mc.thePlayer.posY
            while (posY > ground) {
                sendPacket(C04PacketPlayerPosition(mc.thePlayer.posX, posY, mc.thePlayer.posZ, true))
                if (posY - 8.0 < ground) break // Prevent next step
                posY -= 8.0
            }
        }
        sendPacket(C04PacketPlayerPosition(mc.thePlayer.posX, ground, mc.thePlayer.posZ, true))
        var posY = ground
        while (posY < mc.thePlayer.posY) {
            sendPacket(C04PacketPlayerPosition(mc.thePlayer.posX, posY, mc.thePlayer.posZ, true))
            if (posY + 8.0 > mc.thePlayer.posY) break // Prevent next step
            posY += 8.0
        }
        sendPacket(C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true))
        groundTimer.reset()
    }

    // TODO: Make better and faster calculation lol
    private fun calculateGround(): Double {
        val playerBoundingBox = mc.thePlayer.entityBoundingBox
        var blockHeight = 0.05
        var ground = mc.thePlayer.posY
        while (ground > 0.0) {
            val customBox = AxisAlignedBB.fromBounds(
                playerBoundingBox.maxX,
                ground + blockHeight,
                playerBoundingBox.maxZ,
                playerBoundingBox.minX,
                ground,
                playerBoundingBox.minZ
            )
            if (mc.theWorld.checkBlockCollision(customBox)) {
                if (blockHeight <= 0.05) return ground + blockHeight
                ground += blockHeight
                blockHeight = 0.05
            }
            ground -= blockHeight
        }
        return 0.0
    }

    override val tag
        get() = mode

    private val modeModule
        get() = flyModes.find { it.modeName == mode }!!
}
