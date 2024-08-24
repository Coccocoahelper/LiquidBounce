/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.item;

import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura;
import net.ccbluex.liquidbounce.features.module.modules.movement.NoSlow;
import net.ccbluex.liquidbounce.features.module.modules.render.Animation;
import net.ccbluex.liquidbounce.features.module.modules.render.Animations;
import net.ccbluex.liquidbounce.features.module.modules.render.AntiBlind;
import net.ccbluex.liquidbounce.utils.render.FakeItemRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.AbstractClientPlayerEntityEntity;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.client.render.GlStateManager;
import net.minecraft.client.render.ItemRenderer;
import net.minecraft.client.render.RenderHelper;
import net.minecraft.client.render.block.model.ItemCameraTransforms;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static net.minecraft.client.render.GlStateManager.*;

@Mixin(ItemRenderer.class)
@SideOnly(Side.CLIENT)
public abstract class MixinItemRenderer {

    @Shadow
    private float prevEquippedProgress;

    @Shadow
    private float equippedProgress;

    @Shadow
    @Final
    private Minecraft mc;
    @Shadow
    private ItemStack itemToRender;

    @Shadow
    protected abstract void rotateArroundXAndY(float angle, float angleY);

    @Shadow
    protected abstract void setLightMapFromPlayer(AbstractClientPlayerEntity clientPlayer);

    @Shadow
    protected abstract void rotateWithPlayerRotations(ClientPlayerEntity entityplayerspIn, float partialTicks);

    @Shadow
    protected abstract void renderItemMap(AbstractClientPlayerEntity clientPlayer, float pitch, float equipmentProgress, float swingProgress);

    @Shadow
    protected abstract void transformFirstPersonItem(float equipProgress, float swingProgress);

    @Shadow
    protected abstract void performDrinking(AbstractClientPlayerEntity clientPlayer, float partialTicks);

    @Shadow
    protected abstract void doBowTransformations(float partialTicks, AbstractClientPlayerEntity clientPlayer);

    @Shadow
    protected abstract void doItemUsedTransformations(float swingProgress);

    @Shadow
    public abstract void renderItem(LivingEntity entityIn, ItemStack heldStack, ItemCameraTransforms.TransformType transform);

    @Shadow
    protected abstract void renderPlayerArm(AbstractClientPlayerEntity clientPlayer, float equipProgress, float swingProgress);

    @Shadow
    private int equippedItemSlot = -1;

    /**
     * @author SuperSkidder
     * @reason Make fake items render correctly
     */
    @Overwrite
    public void updateEquippedItem() {
        this.prevEquippedProgress = this.equippedProgress;
        EntityPlayer entityplayer = this.mc.thePlayer;
        ItemStack itemstack = entityplayer.inventory.getCurrentItem();
        if (FakeItemRender.INSTANCE.getFakeItem() != -1) {
            itemstack = entityplayer.inventory.getStackInSlot(FakeItemRender.INSTANCE.getFakeItem());
        }
        boolean flag = false;
        if (this.itemToRender != null && itemstack != null) {
            if (!this.itemToRender.getIsItemStackEqual(itemstack)) {
                if (!this.itemToRender.getItem().shouldCauseReequipAnimation(this.itemToRender, itemstack, this.equippedItemSlot != entityplayer.inventory.currentItem)) {
                    this.itemToRender = itemstack;
                    this.equippedItemSlot = entityplayer.inventory.currentItem;
                    return;
                }

                flag = true;
            }
        } else if (this.itemToRender == null && itemstack == null) {
            flag = false;
        } else {
            flag = true;
        }

        float f = 0.4F;
        float f1 = flag ? 0.0F : 1.0F;
        float f2 = MathHelper.clamp_float(f1 - this.equippedProgress, -f, f);
        this.equippedProgress += f2;
        if (this.equippedProgress < 0.1F) {
            this.itemToRender = itemstack;
            this.equippedItemSlot = entityplayer.inventory.currentItem;
        }

    }

    /**
     * @author CCBlueX
     */
    @Overwrite
    public void renderItemInFirstPerson(float partialTicks) {
        final KillAura killAura = KillAura.INSTANCE;
        final NoSlow noSlow = NoSlow.INSTANCE;
        final Animations animations = Animations.INSTANCE;

        float f = 1f - (prevEquippedProgress + (equippedProgress - prevEquippedProgress) * partialTicks);
        ClientPlayerEntity abstractclientplayer = mc.thePlayer;
        float f1 = abstractclientplayer.getSwingProgress(partialTicks);
        float f2 = abstractclientplayer.prevRotationPitch + (abstractclientplayer.rotationPitch - abstractclientplayer.prevRotationPitch) * partialTicks;
        float f3 = abstractclientplayer.prevRotationYaw + (abstractclientplayer.rotationYaw - abstractclientplayer.prevRotationYaw) * partialTicks;
        rotateArroundXAndY(f2, f3);
        setLightMapFromPlayer(abstractclientplayer);
        rotateWithPlayerRotations(abstractclientplayer, partialTicks);
        enableRescaleNormal();
        pushMatrix();

        if (animations.handleEvents()) {
            float scale = animations.getHandItemScale();
            float x = animations.getHandX();
            float y = animations.getHandY();
            float rotX = animations.getHandPosX();
            float rotY = animations.getHandPosY();
            float rotZ = animations.getHandPosZ();

            translate(x, y, scale);
            rotate(rotX, 1f, 0f, 0f);
            rotate(rotY, 0f, 1f, 0f);
            rotate(rotZ, 0f, 0f, 1f);
        }

        if (itemToRender != null) {
            boolean isForceBlocking = (itemToRender.getItem() instanceof ItemSword && !killAura.getAutoBlock().equals("Off") &&
                    (killAura.getRenderBlocking() || killAura.getTarget() != null && (killAura.getBlinkAutoBlock() || killAura.getForceBlockRender()))
                    || noSlow.isUNCPBlocking());

            if (itemToRender.getItem() instanceof ItemMap) {
                renderItemMap(abstractclientplayer, f2, f, f1);
            } else if (abstractclientplayer.getItemInUseCount() > 0 || isForceBlocking) {
                EnumAction enumaction = isForceBlocking ? EnumAction.BLOCK : itemToRender.getItemUseAction();

                switch (enumaction) {
                    case NONE:
                        transformFirstPersonItem(f, 0f);
                        break;
                    case EAT:
                    case DRINK:
                        performDrinking(abstractclientplayer, partialTicks);
                        transformFirstPersonItem(f, f1);
                        break;
                    case BLOCK:
                        final Animation animation;

                        if (animations.handleEvents()) {
                            animation = animations.getAnimation();
                        } else { // Use 1.7 animation
                            animation = animations.getDefaultAnimation();
                        }

                        if (animation != null) {
                            animation.transform(f1, f, abstractclientplayer);
                        }
                        break;
                    case BOW:
                        transformFirstPersonItem(f, f1);
                        doBowTransformations(partialTicks, abstractclientplayer);
                        break;
                }
            } else {
                if (!animations.handleEvents() || !animations.getOddSwing()) {
                    doItemUsedTransformations(f1);
                }

                transformFirstPersonItem(f, f1);
            }

            renderItem(abstractclientplayer, itemToRender, ItemCameraTransforms.TransformType.FIRST_PERSON);
        } else if (!abstractclientplayer.isInvisible()) {
            renderPlayerArm(abstractclientplayer, f, f1);
        }

        popMatrix();
        disableRescaleNormal();
        RenderHelper.disableStandardItemLighting();
    }


    @Redirect(method = "renderFireInFirstPerson", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;color(FFFF)V"))
    private void renderFireInFirstPerson(float p_color_0_, float p_color_1_, float p_color_2_, float p_color_3_) {
        final AntiBlind antiBlind = AntiBlind.INSTANCE;
        if (p_color_3_ != 1F && antiBlind.handleEvents()) {
            GlStateManager.color(p_color_0_, p_color_1_, p_color_2_, antiBlind.getFireEffect());
        } else {
            GlStateManager.color(p_color_0_, p_color_1_, p_color_2_, p_color_3_);
        }
    }
}
