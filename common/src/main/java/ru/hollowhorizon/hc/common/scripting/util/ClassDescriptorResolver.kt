package ru.hollowhorizon.hc.common.scripting.util

import imgui.ImGui
import imgui.extension.texteditor.TextEditor
import imgui.flag.ImGuiCol
import kotlinx.serialization.Serializable
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.js.descriptorUtils.nameIfStandardType
import org.jetbrains.kotlin.types.DeferredType
import org.jetbrains.kotlin.types.FlexibleType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.error.ErrorType
import org.jetbrains.kotlin.types.error.ErrorTypeConstructor
import ru.hollowhorizon.hc.api.utils.Polymorphic
import ru.hollowhorizon.hc.client.imgui.FontAwesomeIcons

@Serializable
@Polymorphic(CodeCompletion::class)
data class MethodDescriptor(val name: String, val parameters: List<String>, val returnType: String) : CodeCompletion {
    override fun complete(editor: TextEditor) {
        val lines = editor.textLines
        val original = lines[editor.cursorPositionLine]
        if ('.' in original) {
            lines[editor.cursorPositionLine] = original.substringBeforeLast(".") + "." + name + "()"
        } else {
            lines[editor.cursorPositionLine] = original.substringBeforeLast(' ') + " $name()"
        }
        editor.textLines = lines
        editor.setCursorPosition(editor.cursorPositionLine, lines[editor.cursorPositionLine].length)

        if (parameters.isNotEmpty()) editor.setCursorPosition(
            editor.cursorPositionLine,
            editor.cursorPositionColumn - 1
        )
    }

    override fun draw(): Boolean {
        ImGui.pushStyleColor(ImGuiCol.Text, 0.61f, 0.35f, 0.56f, 1f)
        val result = ImGui.selectable(FontAwesomeIcons.Cog + " $name(${parameters.joinToString(", ")}): $returnType")
        ImGui.popStyleColor()

        return result
    }

    override fun toString(): String {
        return FontAwesomeIcons.Cog + " $name(${parameters.joinToString(", ")}): $returnType"
    }

}

@Serializable
@Polymorphic(CodeCompletion::class)
class FieldDescriptor(val name: String, private val returnType: String) : CodeCompletion {
    override fun complete(editor: TextEditor) {
        val lines = editor.textLines
        val original = lines[editor.cursorPositionLine]
        if ('.' in original) {
            lines[editor.cursorPositionLine] = original.substringBeforeLast(".") + "." + name
        } else {
            lines[editor.cursorPositionLine] = original.substringBeforeLast(' ') + " $name"
        }
        editor.textLines = lines
        editor.setCursorPosition(editor.cursorPositionLine, lines[editor.cursorPositionLine].length)
    }

    override fun draw(): Boolean {
        ImGui.pushStyleColor(ImGuiCol.Text, 0.25f, 0.56f, 0.96f, 1f)
        val result = ImGui.selectable(FontAwesomeIcons.Cube + " " + name + ": " + returnType)
        ImGui.popStyleColor()
        return result
    }

    override fun toString(): String {
        return FontAwesomeIcons.Cube + " $name: $returnType"
    }
}

@Serializable
@Polymorphic(CodeCompletion::class)
class ImportDescriptor(val name: String) : CodeCompletion {
    override fun complete(editor: TextEditor) {
        val lines = editor.textLines
        val original = lines[editor.cursorPositionLine]
        val separator = if ('.' in original) "." else " "
        lines[editor.cursorPositionLine] = original.substringBeforeLast(separator) + separator + name
        editor.textLines = lines
        editor.setCursorPosition(editor.cursorPositionLine, lines[editor.cursorPositionLine].length)
    }

    override fun draw(): Boolean {
        ImGui.pushStyleColor(ImGuiCol.Text, 0.45f, 0.46f, 0.96f, 1f)
        val result = ImGui.selectable(FontAwesomeIcons.FolderOpen + " " + name)
        ImGui.popStyleColor()
        return result
    }

    override fun toString(): String {
        return FontAwesomeIcons.FolderOpen + " " + name
    }
}

@Serializable
@Polymorphic(CodeCompletion::class)
class ClassCompletionDescriptor(val name: String) : CodeCompletion {
    override fun complete(editor: TextEditor) {
        val lines = editor.textLines
        val original = lines[editor.cursorPositionLine]
        if ('.' in original) {
            lines[editor.cursorPositionLine] = original.substringBeforeLast(".") + "." + name
        } else {
            lines[editor.cursorPositionLine] = name
        }
        editor.textLines = lines
        editor.setCursorPosition(editor.cursorPositionLine, lines[editor.cursorPositionLine].length)
    }

    override fun draw(): Boolean {
        ImGui.pushStyleColor(ImGuiCol.Text, 0.25f, 0.86f, 0.46f, 1f)
        val result = ImGui.selectable(FontAwesomeIcons.Cube + " " + name)
        ImGui.popStyleColor()
        return result
    }

    override fun toString(): String {
        return FontAwesomeIcons.Cube + " " + name
    }

}

interface CodeCompletion {
    /**
     * Applying code completion to text.
     */
    fun complete(editor: TextEditor)

    /**
     * Draws code completion.
     * @return return true to apply completion
     */
    fun draw(): Boolean
}


val KotlinType.simpleClassName: String
    get() {
        return when (this) {
            is SimpleType -> {
                if (this is ErrorType) {
                    val type = (constructor as? ErrorTypeConstructor)?.formatParams?.firstOrNull()
                    if(type != null) return "Error Resolve Type ($type)"
                }

                val type = constructor.declarationDescriptor?.name?.asString()
                    ?: nameIfStandardType?.asString()
                    ?: "Unit"

                val parameters = arguments.joinToString(", ") { it.type.simpleClassName }

                val nullable = isMarkedNullable

                return type + if (parameters.isNotEmpty()) "<$parameters>" else "" + if (nullable) "?" else ""
            }

            is DeferredType -> this.delegate.simpleClassName
            is FlexibleType -> lowerBound.simpleClassName
            else -> TypeUtils.getClassDescriptor(this)?.name?.asString() ?: "???"
        }
    }

fun ClassDescriptor.getMethodsAndVariables(isStatic: Boolean): List<CodeCompletion> {
    val scope = if (isStatic) staticScope else unsubstitutedMemberScope

    val methods = scope.getContributedDescriptors()
        .filterIsInstance<org.jetbrains.kotlin.descriptors.FunctionDescriptor>()
        .filter { it.visibility == DescriptorVisibilities.PUBLIC }
        .map {
            val result = it.returnType?.simpleClassName ?: "???"
            val args = it.valueParameters.map { it.name.asString() + ": " + it.type.simpleClassName }
            MethodDescriptor(it.name.asString(), args, result)
        }

    val variables = scope.getContributedDescriptors()
        .filterIsInstance<org.jetbrains.kotlin.descriptors.PropertyDescriptor>()
        .filter { it.visibility == DescriptorVisibilities.PUBLIC }
        .map {
            val result = it.returnType?.simpleClassName ?: "???"
            FieldDescriptor(it.name.asString(), result)
        }

    return methods + variables
}
