import { memo } from "react";
import { Handle, Position, type NodeProps } from "reactflow";
import "./styles/react-flow-styles.css";

type Port = {
    id: string;
    label: string;
    type: 'exec' | 'data';
    dataType?: 'int' | 'string' | 'bool' | 'object' | 'array';
};

type Data = {
    title: string;
    category: 'branch';
    inputs?: Port[];
    outputs?: Port[];
};

function BranchNode(props: NodeProps<Data>) {
    const { data, selected } = props;

    const getHandleClass = (port: Port) => {
        if (port.type === 'exec') {
            return 'rf-handle-exec';
        }
        switch (port.dataType) {
            case 'int': return 'rf-handle-int';
            case 'string': return 'rf-handle-string';
            case 'bool': return 'rf-handle-bool';
            case 'object': return 'rf-handle-object';
            case 'array': return 'rf-handle-array';
            default: return 'rf-handle-data';
        }
    };

    return (
        <div className={`rf-node branch-node ${selected ? "selected" : ""}`}>
            <div className="rf-node-header branch-header">
                {data.title}
            </div>

            <div className="rf-node-body">
                <div className="rf-node-ports">
                    {/* 输入列 */}
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

                    {/* 输出列 */}
                    <div className="rf-node-column right">
                        {data.outputs?.map((port) => (
                            <div key={port.id} className="rf-port rf-port-right">
                                <span className="rf-port-label">{port.label}</span>
                                <Handle
                                    type="source"
                                    position={Position.Right}
                                    id={port.id}
                                    className={`rf-handle ${getHandleClass(port)}`}
                                />
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}

export default memo(BranchNode);