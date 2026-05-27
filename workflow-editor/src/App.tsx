import { useCallback } from "react";
import {
    Background,
    Controls,
    MiniMap,
    ReactFlow,
    addEdge,
    useEdgesState,
    useNodesState,
    type Node,
    type Edge
} from "reactflow";
import { nodetypes } from "./nodetype/types.ts";
import "reactflow/dist/style.css";

export const initialNodes: Node[] = [
    {
        id: "start-1",
        type: "start",
        position: { x: 100, y: 100 },
        data: {
            title: "开始",
            outputs: [
                {
                    id: "out-exec",
                    label: "执行"
                }
            ],
        },
    },
    {
        id: "end-1",
        type: "end",
        position: { x: 500, y: 100 },
        data: {
            title: "结束",
            inputs: [
                {
                    id: "in-exec",
                    label: "完成"
                }
            ],
        },
    },
];

const initialEdges = [
    {
        id: "e1-2",
        source: "1",
        target: "2"
    }
];

export default function App() {
    const [nodes, _setNodes, onNodesChange] =
        useNodesState(initialNodes);

    const [edges, setEdges, onEdgesChange] =
        useEdgesState(initialEdges);

    const onConnect = useCallback(
        (params: any) => {
            if (
                params.sourceHandle === 'out-exec' &&
                params.targetHandle === 'in-exec'
            ) {
                setEdges((eds) => addEdge(params, eds));
            }
        },
        [setEdges]
    );

    const onEdgeClick = (event: React.MouseEvent, edge: Edge) => {
        event.preventDefault();
        setEdges((eds) =>
            eds.filter((e) => e.id !== edge.id));
    };

    return (
        <div style={{position: 'fixed', inset: 0, background: '#fff'}}>
            <ReactFlow
                nodes={nodes}
                edges={edges}
                nodeTypes={nodetypes}
                onNodesChange={onNodesChange}
                onEdgesChange={onEdgesChange}
                onConnect={onConnect}
                onEdgeContextMenu={onEdgeClick}
                defaultEdgeOptions={{
                    style: {
                        strokeWidth: 3
                    }
                }}
                fitView
            >
                <Background/>
                <MiniMap/>
                <Controls/>
            </ReactFlow>
        </div>
    );
}