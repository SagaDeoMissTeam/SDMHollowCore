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

package ru.hollowhorizon.hc.client.models.internal

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.ItemInHandRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.opengl.GL33
import ru.hollowhorizon.hc.client.handlers.TickHandler
import ru.hollowhorizon.hc.client.models.gltf.CURRENT_NORMAL
import ru.hollowhorizon.hc.client.models.gltf.NODE_GLOBAL_TRANSFORMATION_LOOKUP_CACHE
import ru.hollowhorizon.hc.client.models.internal.animations.GLTFAnimationPlayer
import ru.hollowhorizon.hc.client.models.internal.manager.AnimatedEntityCapability
import ru.hollowhorizon.hc.client.models.internal.manager.GltfManager
import ru.hollowhorizon.hc.client.utils.shouldOverrideShaders
import ru.hollowhorizon.hc.common.registry.ModShaders

//? if <=1.19.2 {
/*import ru.hollowhorizon.hc.client.utils.fromMc
*///?}

class ModelData(
    val leftHand: ItemStack?,
    val rightHand: ItemStack?,
    val itemInHandRenderer: ItemInHandRenderer?,
    val entity: LivingEntity?,
)

typealias NodeRenderer = (LivingEntity, PoseStack, Node, MultiBufferSource, Int) -> Unit

class AnimatedModel(val modelTree: Model) {
    val nodes = modelTree.walkNodes().associateBy { (it.name ?: "Unnamed") }
    val animationPlayer = GLTFAnimationPlayer(this)
    var visuals: NodeRenderer = { _, _, _, _, _ -> }

    fun update(capability: AnimatedEntityCapability, currentTick: Int, partialTick: Float) {
        animationPlayer.setTick(currentTick)
        animationPlayer.update(capability, partialTick)
    }

    fun entityUpdate(entity: LivingEntity, capability: AnimatedEntityCapability, partialTick: Float) {
        animationPlayer.updateEntity(entity, capability, partialTick)
    }

    fun render(
        stack: PoseStack,
        modelData: ModelData,
        consumer: (ResourceLocation) -> Int,
        source: MultiBufferSource,
        light: Int,
        overlay: Int,
    ) {
        modelTree.scenes.forEach {
            it.nodes.forEach { node ->
                node.renderDecorations(stack, visuals, modelData, source, light)
            }
        }

        val activeTexture = GlStateManager._getActiveTexture()

        //Получение текущих VAO и IBO
        val currentVAO = GL33.glGetInteger(GL33.GL_VERTEX_ARRAY_BINDING)
        val currentElementArrayBuffer = GL33.glGetInteger(GL33.GL_ELEMENT_ARRAY_BUFFER_BINDING)

        //? if >=1.20.1 {
        CURRENT_NORMAL = stack.last().normal()
        //?} else {
        /*CURRENT_NORMAL = stack.last().normal().fromMc()
        *///?}

        transformSkinning(stack)


        GL33.glVertexAttrib4f(1, 1.0F, 1.0F, 1.0F, 1.0F) // Цвет
        GL33.glVertexAttribI2i(3, overlay and '\uffff'.code, overlay shr 16 and '\uffff'.code) // Оверлей при ударе
        GL33.glVertexAttribI2i(4, light and '\uffff'.code, light shr 16 and '\uffff'.code) // Освещение

        GlStateManager._activeTexture(GL33.GL_TEXTURE2)
        val texture2 = GlStateManager.TEXTURES[GlStateManager.activeTexture].binding
        GlStateManager._bindTexture(GltfManager.lightTexture.id)
        GlStateManager._activeTexture(GL33.GL_TEXTURE1)
        val texture1 = GlStateManager.TEXTURES[GlStateManager.activeTexture].binding
        Minecraft.getInstance().gameRenderer.overlayTexture().setupOverlayColor()
        GlStateManager._bindTexture(RenderSystem.getShaderTexture(1))
        Minecraft.getInstance().gameRenderer.overlayTexture().teardownOverlayColor()
        GlStateManager._activeTexture(GL33.GL_TEXTURE0)

        val texture = GlStateManager.TEXTURES[GlStateManager.activeTexture].binding

        drawWithShader(SHADER) {
            modelTree.scenes.forEach { it.render(stack, visuals, modelData, consumer, light) }
        }

        GlStateManager._activeTexture(GL33.GL_TEXTURE2)

        GlStateManager._bindTexture(texture2)
        GlStateManager._activeTexture(GL33.GL_TEXTURE1)
        GlStateManager._bindTexture(texture1)
        GlStateManager._activeTexture(GL33.GL_TEXTURE0)
        GlStateManager._bindTexture(texture)
        GlStateManager._activeTexture(activeTexture)

        GL33.glBindVertexArray(currentVAO)
        GL33.glBindBuffer(GL33.GL_ELEMENT_ARRAY_BUFFER, currentElementArrayBuffer)
        GlStateManager._glUseProgram(0)

        NODE_GLOBAL_TRANSFORMATION_LOOKUP_CACHE.clear()
    }

    private fun transformSkinning(stack: PoseStack) {
        GL33.glUseProgram(GltfManager.glProgramSkinning)
        GL33.glEnable(GL33.GL_RASTERIZER_DISCARD)
        modelTree.scenes.forEach { it.transformSkinning(stack) }
        GL33.glBindBuffer(GL33.GL_TEXTURE_BUFFER, 0)
        GL33.glDisable(GL33.GL_RASTERIZER_DISCARD)
    }

    fun destroy() {
        modelTree.walkNodes().mapNotNull { it.mesh }.flatMap { it.primitives }.forEach(Primitive::destroy)
    }

    fun findPosition(name: String, entity: LivingEntity): Matrix4f? {
        val node = nodes[name] ?: return null
        var lerpBodyRot = -Mth.rotLerp(TickHandler.partialTick, entity.yBodyRotO, entity.yBodyRot)
        val YP = Vector3f(0.0f, 1.0f, 0.0f)

        lerpBodyRot *= 0.017453292f

        return Matrix4f().rotation(Quaternionf(0f, Mth.sin(lerpBodyRot / 2.0f), 0f, Mth.cos(lerpBodyRot / 2.0f)))
            .mul(node.globalMatrix)
    }

    fun findRotation(name: String): Quaternionf {
        val node = nodes[name] ?: return Quaternionf(0.0f, 0.0f, 0.0f, 1.0f)
        return node.globalRotation
    }

    companion object {
        val SHADER get() =
            if (shouldOverrideShaders()) GameRenderer.getRendertypeEntityCutoutShader()!!
            else ModShaders.GLTF_ENTITY // Ванильный шейдер не поддерживает матрицу нормалей
    }
}