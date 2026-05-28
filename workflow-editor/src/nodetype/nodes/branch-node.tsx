import type {NodeDefinition} from "../types/node";
import {PortDataType, type PortDefinition, PortDirection, PortType} from "../types/port";

function createEventNode(
    title: string,
    type: string,
    inputs: PortDefinition[],
    outputs: PortDefinition[]
): NodeDefinition {
    return {
        title,
        type,
        headerClass: "branch-header",
        inputs,
        outputs,
    }
}

export const IfNode: NodeDefinition = createEventNode("比较器", "if", [
    {
        id: "in-exec",
        name: "EXEC IN",
        type: PortType.Exec,
        direction: PortDirection.Input
    },
    {
        id: "in-cond-a",
        name: "A值",
        type: PortType.Data,
        dataType: PortDataType.Object,
        direction: PortDirection.Input
    },
    {
        id: "in-cond-b",
        name: "B值",
        type: PortType.Data,
        dataType: PortDataType.Object,
        direction: PortDirection.Input
    },
], [
    {
        id: "out-exec-1",
        name: "是",
        type: PortType.Exec,
        direction: PortDirection.Output
    },
    {
        id: "out-exec-2",
        name: "否",
        type: PortType.Exec,
        direction: PortDirection.Output
    },
]);

export const IsNullNode: NodeDefinition = createEventNode("是否为空对象", "ifnull", [
    {
        id: "in-exec",
        name: "EXEC IN",
        type: PortType.Exec,
        direction: PortDirection.Input
    },
    {
        id: "in-object",
        name: "输入对象",
        type: PortType.Data,
        dataType: PortDataType.Object,
        direction: PortDirection.Input
    },
], [
    {
        id: "out-exec-1",
        name: "是",
        type: PortType.Exec,
        direction: PortDirection.Output
    },
    {
        id: "out-exec-2",
        name: "否",
        type: PortType.Exec,
        direction: PortDirection.Output
    },
    {
        id: "out-object",
        name: "输出对象",
        type: PortType.Data,
        dataType: PortDataType.Object,
        direction: PortDirection.Output
    },
]);