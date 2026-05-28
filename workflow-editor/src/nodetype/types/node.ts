import type { PortDefinition } from "./port.ts";

export interface NodeDefinition {
    type: string;
    title: string;
    headerClass: string;
    inputs: PortDefinition[];
    outputs: PortDefinition[];
}