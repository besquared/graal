/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.graal.nodes;

import com.oracle.graal.compiler.common.type.Stamp;
import com.oracle.graal.compiler.common.type.StampFactory;
import com.oracle.graal.compiler.common.type.TypeReference;
import com.oracle.graal.graph.Node;
import com.oracle.graal.graph.NodeClass;
import com.oracle.graal.graph.spi.CanonicalizerTool;
import com.oracle.graal.nodeinfo.NodeInfo;
import com.oracle.graal.nodes.extended.GuardingNode;

import jdk.vm.ci.meta.ResolvedJavaType;

/**
 * A {@link PiNode} where the type is not yet known. If the type becomes known at a later point in
 * the compilation, this can canonicalize to a regular {@link PiNode}.
 */
@NodeInfo
public final class DynamicPiNode extends PiNode {

    public static final NodeClass<DynamicPiNode> TYPE = NodeClass.create(DynamicPiNode.class);
    @Input ValueNode typeMirror;

    public DynamicPiNode(ValueNode object, GuardingNode guard, ValueNode typeMirror) {
        super(TYPE, object, StampFactory.object(), guard);
        this.typeMirror = typeMirror;
    }

    @Override
    public Node canonical(CanonicalizerTool tool) {
        if (typeMirror.isConstant()) {
            ResolvedJavaType t = tool.getConstantReflection().asJavaType(typeMirror.asConstant());
            if (t != null) {
                Stamp staticPiStamp;
                if (t.isPrimitive()) {
                    staticPiStamp = StampFactory.alwaysNull();
                } else {
                    TypeReference type = TypeReference.createTrusted(tool.getAssumptions(), t);
                    staticPiStamp = StampFactory.object(type);
                }

                return new PiNode(object(), staticPiStamp, (ValueNode) getGuard()).canonical(tool);
            }
        }

        return this;
    }
}