import {useCallback,useEffect} from "react";
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
import {
    GetConnectionNode,
    GetDatabaseNode
} from "./nodetype/nodes/action-node";
import {IsNullNode} from "./nodetype/nodes/branch-node";
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
        position: {x: 800, y: 100},
        data: {definition: GetConnectionNode},
    },
    {
        id: "4",
        type: "bp",
        position: {x: 900, y: 300},
        data: {definition: GetDatabaseNode},
    },
    {
        id: "5",
        type: "bp",
        position: {x: 1300, y: 350},
        data: {definition: IsNullNode},
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

    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if ((e.ctrlKey || e.metaKey) && e.key === 's') {
                e.preventDefault();
                console.log('导出流程图数据：', {nodes, edges,});
            }
        };
        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [nodes, edges]);

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