package ru.hollowhorizon.hc.client.models.gltf

import com.mojang.math.Matrix4f
import com.mojang.math.Quaternion
import com.mojang.math.Vector3f
import org.lwjgl.opengl.GL11
import java.nio.FloatBuffer

data class Transformation(
    val translationX: Float,
    val translationY: Float,
    val translationZ: Float,
    val rotationX: Float,
    val rotationY: Float,
    val rotationZ: Float,
    val rotationW: Float,
    val scaleX: Float,
    val scaleY: Float,
    val scaleZ: Float,
    private val matrix: Matrix4f = Matrix4f().apply(Matrix4f::setIdentity)
) {

    val translation: Vector3f get() = Vector3f(translationX, translationY, translationZ)
    val rotation: Quaternion get() = Quaternion(rotationX, rotationY, rotationZ, rotationW)
    val scale: Vector3f get() = Vector3f(scaleX, scaleY, scaleZ)

    @JvmOverloads
    constructor(
        translation: Vector3f = Vector3f(),
        rotation: Quaternion = Quaternion(0.0f, 0.0f, 0.0f, 1.0f),
        scale: Vector3f = Vector3f(1.0f, 1.0f, 1.0f),
        matrix: Matrix4f = Matrix4f().apply(Matrix4f::setIdentity)
    ) : this(
        translation.x(), translation.y(), translation.z(),
        rotation.i(), rotation.j(), rotation.k(), rotation.r(),
        scale.x(), scale.y(), scale.z(),
        matrix
    )

    fun getMatrix(): Matrix4f {
        val m = matrix.copy()

        // rotation
        if (rotationW != 0f && !(rotationX == 0f && rotationY == 0f && rotationZ == 0f && rotationW == 1f)) {
            m.multiply(rotation.apply {
                //Invert
                val invNorm = 1.0f / (i() * i() + j() * j() + k() * k() + r() * r())
                set(i() * invNorm, j() * invNorm, k() * invNorm, r() * invNorm)
            })
        }
        // translation
        m.m30 += translationX
        m.m31 += translationY
        m.m32 += translationZ

        // scale
        m.m00 *= scaleX
        m.m01 *= scaleX
        m.m02 *= scaleX
        m.m10 *= scaleY
        m.m11 *= scaleY
        m.m12 *= scaleY
        m.m20 *= scaleZ
        m.m21 *= scaleZ
        m.m22 *= scaleZ

        return m
    }

    fun lerp(other: Transformation, step: Float): Transformation {
        return Transformation(
            translation = this.translation.interpolated(other.translation, step),
            rotation = this.rotation.interpolated(other.rotation, step),
            scale = this.scale.interpolated(other.scale, step)
        )
    }

    companion object {
        val IDENTITY: Transformation = Transformation()
    }
}

private fun Vector3f.interpolated(other: Vector3f, step: Float): Vector3f {
    return Vector3f(
        this.x() + (other.x() - this.x()) * step,
        this.y() + (other.y() - this.y()) * step,
        this.z() + (other.z() - this.z()) * step,
    )
}

private fun Quaternion.interpolated(other: Quaternion, step: Float): Quaternion {
    return Quaternion(
        this.i() + (other.i() - this.i()) * step,
        this.j() + (other.j() - this.j()) * step,
        this.k() + (other.k() - this.k()) * step,
        this.r() + (other.r() - this.r()) * step,
    )
}
