import { useState, useCallback } from 'react';
import {
    ReactFlow,
    applyNodeChanges,
    applyEdgeChanges,
    addEdge,
    Background,
    MiniMap,
    type Node,
    type NodeChange,
    type EdgeChange,
    type Connection
} from 'reactflow';
import 'reactflow/dist/style.css';
import type { NodeData } from './nodes/types';
import { myNodeTypes } from './nodes';

const initialNodes = [
    {
        id: '1',
        type: 'custom',
        position: { x: 100, y: 100 },
        data: { label: '节点1', content: 'Hell Node 1' },
    },
    {
        id: '2',
        type: 'custom',
        position: { x: 300, y: 200 },
        data: { label: '节点2', content: 'Hell Node 2' },
    },
];

const initialEdges = [
    {
        id: 'e1-2',
        source: '1',
        target: '2',
    },
];

export default function App() {
    const [nodes, setNodes] = useState<Node<NodeData>[]>(initialNodes);
    const [edges, setEdges] = useState(initialEdges);

    const onNodesChange = useCallback(
        (changes: NodeChange[]) => {
            setNodes((nds) => applyNodeChanges(changes, nds));
        },
        []
    );

    const onEdgesChange = useCallback(
        (changes: EdgeChange[]) => {
            setEdges((eds) => applyEdgeChanges(changes, eds));
        },
        []
    );

    const onConnect = useCallback(
        (params: Connection) => {
            setEdges((eds) => addEdge(params, eds));
        },
        []
    );

    return (
        <div style={{ position: 'fixed', inset: 0, background: '#fff' }}>
            <ReactFlow
                nodes={nodes}
                edges={edges}
                nodeTypes={myNodeTypes}
                selectionKeyCode={'Shift'}
                onNodesChange={onNodesChange}
                onEdgesChange={onEdgesChange}
                onConnect={onConnect}
                fitView
            >
                <Background />
                <MiniMap />
            </ReactFlow>
        </div>
    );
}