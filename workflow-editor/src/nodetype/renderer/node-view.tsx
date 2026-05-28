import {Handle, Position} from "reactflow";

import type {NodeProps} from "reactflow";
import type {NodeDefinition} from "../types/node";
import {PortDataType, PortType, type PortDefinition} from "../types/port";
import {PortControlView} from "./port-control-view";

import "../styles/react-flow-style.css";

type NodeData = {
    definition: NodeDefinition;
};

export function NodeView({data, selected,}: NodeProps<NodeData>) {

    const definition = data.definition;

    const getHandleClass = (port: PortDefinition) => {
        if (port.type == PortType.Exec)
            return 'rf-handle-exec';
        switch (port.dataType) {
            case PortDataType.Int:
                return 'rf-handle-int';
            case PortDataType.String:
                return 'rf-handle-string';
            case PortDataType.Bool:
                return 'rf-handle-bool';
            case PortDataType.Object:
                return 'rf-handle-object';
            case PortDataType.Array:
                return 'rf-handle-array';
            default:
                return 'rf-handle-data';
        }
    };

    return (
        <div
            className={`rf-node ${selected ? "selected" : ""}`}
        >
            <div
                className={`
                    rf-node-header
                    ${definition.headerClass ?? ""}
                `}
            >
                {definition.title}
            </div>

            <div className="rf-node-body">
                <div className="rf-node-ports">
                    <div className="rf-node-column left">
                        {definition.inputs.map((port) => (
                            <div
                                key={port.id}
                                className="
                                    rf-port
                                    rf-port-left
                                "
                            >
                                <Handle
                                    type="target"
                                    position={Position.Left}
                                    id={port.id}
                                    className={`rf-handle ${getHandleClass(port)}`}
                                />

                                <span className="rf-port-label">{port.name}</span>
                                <PortControlView port={port}/>
                            </div>
                        ))}
                    </div>
                    <div className="rf-node-column right">
                        {definition.outputs.map((port) => (
                            <div
                                key={port.id}
                                className="
                                    rf-port
                                    rf-port-right
                                "
                            >
                                <Handle
                                    type="source"
                                    position={Position.Right}
                                    id={port.id}
                                    className={`rf-handle ${getHandleClass(port)}`}
                                />

                                <span className="rf-port-label">{port.name}</span>

                                <PortControlView port={port}/>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}