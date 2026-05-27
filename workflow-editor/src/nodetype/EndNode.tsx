import { memo } from "react";
import { Handle, Position, type NodeProps } from "reactflow";
import "./styles/react-flow-styles.css";

type Port = {
    id: string;
    label: string;
};

type Data = {
    title: string;
    inputs?: Port[];
};

function EndNode(props: NodeProps<Data>) {
    const { data, selected } = props;

    return (
        <div className={`rf-node ${selected ? "selected" : ""}`}>
            <div
                className="rf-node-header"
                style={{ background: "#ef4444", color: "#ffffff" }}
            >
                {data.title}
            </div>

            <div className="rf-node-body">
                <div className="rf-node-ports">
                    <div className="rf-node-column">
                        {data.inputs?.map((port) => (
                            <div key={port.id} className="rf-port rf-port-left">
                                <Handle
                                    type="target"
                                    position={Position.Left}
                                    id={port.id}
                                    className="rf-handle rf-handle-exec"
                                />

                                <span className="rf-port-label">
                                    {port.label}
                                </span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
}

export default memo(EndNode);