package com.society.sunlitindustry.mixin;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IndustryMixinPluginTest {
    @Test
    void aliSchemaDriftLeavesBytecodeUntouched() {
        for (int count : new int[] {0, 2}) {
            var node = tooltip(count);
            var instructions = node.methods.get(0).instructions;
            int size = instructions.size();
            new IndustryMixinPlugin().postApply(node.name, node, "test.AliClientTooltipCacheMixin", null);
            assertEquals(size, instructions.size());
            assertEquals(count, calls(node, "getTooltipCache"));
            assertEquals(0, calls(node, "palette"));
        }
    }

    @Test
    void supportedAliLookupIsPatchedOnce() {
        var node = tooltip(1);
        new IndustryMixinPlugin().postApply(node.name, node, "test.AliClientTooltipCacheMixin", null);
        assertEquals(0, calls(node, "getTooltipCache"));
        assertEquals(1, calls(node, "palette"));
    }

    private static ClassNode tooltip(int lookups) {
        var node = new ClassNode();
        node.name = "com/yanny/aci/tooltip/TooltipNode";
        var method = new MethodNode(Opcodes.ACC_PUBLIC, "empty", "()Lcom/yanny/aci/tooltip/TooltipNode;", null, null);
        for (int i = 0; i < lookups; i++) {
            method.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "com/yanny/aci/manager/CorePluginManager",
                    "serverRegistry", "Lcom/yanny/aci/manager/CoreServerRegistry;"));
            method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "com/yanny/aci/manager/CoreServerRegistry",
                    "getTooltipCache", "()Lcom/yanny/aci/tooltip/TooltipNodePalette;", false));
        }
        node.methods.add(method);
        return node;
    }

    private static int calls(ClassNode node, String name) {
        int count = 0;
        for (var instruction : node.methods.get(0).instructions) {
            if (instruction instanceof MethodInsnNode call && call.name.equals(name)) count++;
        }
        return count;
    }
}
