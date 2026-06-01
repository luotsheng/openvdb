import type {NodeDefinition} from "./types/node.ts";

const registry = new Map<string, NodeDefinition>();

export function registerNode(
    definition: NodeDefinition
) {
    registry.set(definition.type, definition);
}

export function getNode(
    type: string
) {
    return registry.get(type);
}

export function getAllNodes() {
    return [...registry.values()];
}