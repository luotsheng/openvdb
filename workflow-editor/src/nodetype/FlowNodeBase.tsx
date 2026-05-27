import {memo} from "react";
import {Handle, Position, type NodeProps} from "reactflow";
import "./styles/react-flow-styles.css";
import "./styles/node-port-select.css";

export const PortControl = {
    None: 'none',
    Custom: 'custom',
    Select: 'select',
    Input: 'input',
    Password: 'password',
    Checkbox: 'checkbox',
} as const;

export const PortType = {
    Exec: 'exec',
    Data: 'data',
} as const;

export const PortDataType = {
    Any: 'any',
    Int: 'int',
    String: 'string',
    Bool: 'bool',
    Object: 'object',
    Array: 'array',
} as const;

type Property = {
    value?: string;
    label?: string;
}

type Port = {
    id: string;
    label: string;
    portType: typeof PortType[keyof typeof PortType];
    dataType?: typeof PortDataType[keyof typeof PortDataType];
    control: typeof PortControl[keyof typeof PortControl];
    defaultValue?: any;
    options?: Property[];
};

type NodeData = {
    title: string;
    inputs?: Port[];
    outputs?: Port[];
};

function FlowNodeBase({
    data,
    selected,
    nodeClass = "",
    headerClass = ""
}: NodeProps<NodeData> & {
    nodeClass?: string;
    headerClass?: string;
}) {
    const getHandleClass = (port: Port) => {
        if (port.portType === PortType.Exec) return 'rf-handle-exec';
        switch (port.dataType) {
            case PortDataType.Int: return 'rf-handle-int';
            case PortDataType.String: return 'rf-handle-string';
            case PortDataType.Bool: return 'rf-handle-bool';
            case PortDataType.Object: return 'rf-handle-object';
            case PortDataType.Array: return 'rf-handle-array';
            default: return 'rf-handle-data';
        }
    };

    return (
        <div className={`rf-node ${nodeClass} ${selected ? "selected" : ""}`}>
            <div className={`rf-node-header ${headerClass}`}>
                {data.title}
            </div>

            <div className="rf-node-body">
                <div className="rf-node-ports">
                    <div className="rf-node-column left">
                        {data.inputs?.map((port) => (
                            <div key={port.id} className="rf-port rf-port-left">
                                <Handle
                                    type="target"
                                    position={Position.Left}
                                    id={port.id}
                                    className={`rf-handle ${getHandleClass(port)}`}
                                />
                                <span className="rf-port-label">{port.label}</span>
                            </div>
                        ))}
                    </div>

                    <div className="rf-node-column right">
                        {data.outputs?.map((port) => (
                            <div key={port.id} className="rf-port rf-port-right">
                                <Handle
                                    type="source"
                                    position={Position.Right}
                                    id={port.id}
                                    className={`rf-handle ${getHandleClass(port)}`}
                                />
                                <span className="rf-port-label">{port.label}</span>
                                {port.control === "select" && (
                                    <select
                                        value={port.defaultValue ?? ''}
                                        onChange={(e) => {

                                        }}
                                        className="rf-select"
                                    >
                                        {port.options?.map(opt => (
                                            <option key={opt.value} value={opt.value}>
                                                {opt.label}
                                            </option>
                                        ))}
                                    </select>
                                )}
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}

export const StartNode = memo((props: NodeProps<NodeData>) => (
    <FlowNodeBase
        {...props}
        nodeClass="start-node"
        headerClass="start-header"
    />
));

export const EndNode = memo((props: NodeProps<NodeData>) => (
    <FlowNodeBase
        {...props}
        nodeClass="end-node"
        headerClass="end-header"
    />
));

export const ActionNode = memo((props: NodeProps<NodeData>) => (
    <FlowNodeBase
        {...props}
        nodeClass="action-node"
        headerClass="action-header"
    />
));

export const BranchNode = memo((props: NodeProps<NodeData>) => (
    <FlowNodeBase
        {...props}
        nodeClass="branch-node"
        headerClass="branch-header"
    />
));