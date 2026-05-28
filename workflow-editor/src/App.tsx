import {useCallback} from "react";
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
import {blueprintNodeTypes} from "./nodetype/types";
import {StartNode, EndNode} from "./nodetype/nodes/event-node";
import {IfNode} from "./nodetype/nodes/branch-node";
// css
import "reactflow/dist/style.css";

export const initialNodes: Node[] = [
    {
        id: "1",
        type: "bp",
        position: {x: 100, y: 100},
        data: {definition: StartNode},
    },
    {
        id: "2",
        type: "bp",
        position: {x: 500, y: 100},
        data: {definition: EndNode},
    },
    {
        id: "3",
        type: "bp",
        position: {x: 200, y: 400},
        data: {definition: IfNode},
    },
];

const initialEdges: Edge[] = [];

export default function App() {
    const [nodes, _setNodes, onNodesChange] =
        useNodesState(initialNodes);

    const [edges, setEdges, onEdgesChange] =
        useEdgesState(initialEdges);

    const onConnect = useCallback(
        (params: any) => {
            setEdges((eds) => addEdge(params, eds));
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
                nodeTypes={blueprintNodeTypes}
                onNodesChange={onNodesChange}
                onEdgesChange={onEdgesChange}
                onConnect={onConnect}
                onEdgeContextMenu={onEdgeClick}
                defaultEdgeOptions={{
                    style: {strokeWidth: 3}
                }}
                connectionLineStyle={{
                    strokeWidth: 3
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