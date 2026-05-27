import { memo } from "react";
import { Handle, Position, type NodeProps } from "reactflow";
import "./style-flow-node.css";

type Port = {
    id: string;
    label: string;
};

type Data = {
    title: string;
    outputs?: Port[];
};

function StartNode(props: NodeProps<Data>) {
    const { data, selected } = props;

    return (
        <div className={`rf-node ${selected ? "selected" : ""}`}>
            <div
                className="rf-node-header"
                style={{ background: "#1080ff", color: "#ffffff" }}
            >
                {data.title}
            </div>

            <div className="rf-node-body">
                <div className="rf-node-ports">
                    <div className="rf-node-column">
                    </div>
                    <div className="rf-node-column">
                        {data.outputs?.map((port) => (
                            <div key={port.id} className="rf-port rf-port-right">
                                <span className="rf-port-label">
                                    {port.label}
                                </span>
                                <Handle
                                    type="source"
                                    position={Position.Right}
                                    id={port.id}
                                    className="rf-handle"
                                />
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}

export default memo(StartNode);