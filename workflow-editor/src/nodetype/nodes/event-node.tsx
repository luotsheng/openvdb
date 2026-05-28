import type {NodeDefinition} from "../types/node";
import {type PortDefinition, PortDirection, PortType} from "../types/port";

function createEventNode(
    title: string,
    type: string,
    inputs: PortDefinition[],
    outputs: PortDefinition[]
): NodeDefinition {
    return {
        title,
        type,
        headerClass: "event-header",
        inputs,
        outputs,
    }
}

export const StartNode: NodeDefinition = createEventNode("Start Event", "start", [], [
    {
        id: "out-exec",
        name: "开始执行",
        type: PortType.Exec,
        direction: PortDirection.Output
    },
]);

export const EndNode: NodeDefinition = createEventNode("End Event", "end", [
    {
        id: "in-exec",
        name: "结束执行",
        type: PortType.Exec,
        direction: PortDirection.Input
    }
], []);