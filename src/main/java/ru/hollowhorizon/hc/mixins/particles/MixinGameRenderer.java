/*
 * MIT License
 *
 * Copyright (c) 2024 HollowHorizon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ru.hollowhorizon.hc.mixins.particles;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.hollowhorizon.hc.client.render.effekseer.internal.EffekFpvRenderer;
import ru.hollowhorizon.hc.client.render.effekseer.internal.RenderContext;
import ru.hollowhorizon.hc.client.render.effekseer.internal.RenderStateCapture;
import ru.hollowhorizon.hc.client.render.effekseer.render.EffekRenderer;
import static ru.hollowhorizon.hc.client.render.effekseer.render.RenderUtil.pasteToCurrentDepthFrom;

//? if >=1.21 {
import net.minecraft.client.DeltaTracker;
 
//?}

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Shadow
    private boolean renderHand;

    @Inject(method = "renderLevel", at = @At("TAIL"))
    //? if <1.21 {
    /*private void renderLevelTail(float partialTicks, long finishTimeNano, PoseStack poseStack, CallbackInfo ci) {
    *///?} else {
    private void renderLevelTail(DeltaTracker deltaTracker, CallbackInfo ci) {
    //?}

        if (RenderContext.renderLevelDeferred() && RenderStateCapture.LEVEL.hasCapture) {
            RenderStateCapture.LEVEL.hasCapture = false;

            pasteToCurrentDepthFrom(RenderStateCapture.CAPTURED_WORLD_DEPTH_BUFFER);
            assert RenderStateCapture.LEVEL.camera != null;
            EffekRenderer.onRenderWorldLast(
                    //? if <1.21 {
                    /*partialTicks,
                    *///?} else {
                    deltaTracker.getRealtimeDeltaTicks(),
                     //?}
                    RenderStateCapture.LEVEL.pose, RenderStateCapture.LEVEL.projection, RenderStateCapture.LEVEL.camera);
        }
        final var minecraft = Minecraft.getInstance();
        if (RenderContext.renderHandDeferred() && renderHand) {
            if (RenderContext.captureHandDepth()) {
                pasteToCurrentDepthFrom(RenderStateCapture.CAPTURED_HAND_DEPTH_BUFFER);
            }

            assert minecraft.player != null;
            ((EffekFpvRenderer) minecraft.gameRenderer.itemInHandRenderer).hollowcore$renderFpvEffek(
                    //? if <1.21 {
                    /*partialTicks,
                    *///?} else {
                    deltaTracker.getRealtimeDeltaTicks(),
                     //?}
                    minecraft.player);
        }
    }

}
