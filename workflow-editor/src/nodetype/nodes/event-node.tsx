import type {NodeDefinition} from "../types/node";
import {type PortDefinition, PortDirection, PortType} from "../types/port";

function createNode(
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

export const StartNode: NodeDefinition = createNode("Start Event", "start", [], [
    {
        id: "out-exec",
        name: "START",
        type: PortType.Exec,
        direction: PortDirection.Output
    },
]);

export const EndNode: NodeDefinition = createNode("End Event", "end", [
    {
        id: "in-exec",
        name: "END",
        type: PortType.Exec,
        direction: PortDirection.Input
    }
], []);