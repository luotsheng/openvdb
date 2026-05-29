import {useCallback, useEffect, useRef} from "react";
import {
    Background,
    Controls,
    MiniMap,
    ReactFlow,
    useReactFlow,
    addEdge,
    useEdgesState,
    useNodesState,
    type Node,
    type Edge, ReactFlowProvider
} from "reactflow";
import {blueprintNodeTypes} from "./nodetype/types";
import {StartNode, EndNode} from "./nodetype/nodes/event-node";
import {
    GetConnectionNode,
    GetDatabaseNode,
    GetExecuteScript
} from "./nodetype/nodes/action-node";
import {IsNullNode} from "./nodetype/nodes/branch-node";
// css
import "reactflow/dist/style.css";

export const initialNodes: Node[] = [
    {
        id: "1",
        type: "bp",
        selected: false,
        position: {x: 100, y: 100},
        data: {definition: StartNode},
    },
    {
        id: "2",
        type: "bp",
        selected: false,
        position: {x: 500, y: 100},
        data: {definition: EndNode},
    },
    {
        id: "3",
        type: "bp",
        selected: false,
        position: {x: 800, y: 100},
        data: {definition: GetConnectionNode},
    },
    {
        id: "4",
        type: "bp",
        selected: false,
        position: {x: 900, y: 300},
        data: {definition: GetDatabaseNode},
    },
    {
        id: "5",
        type: "bp",
        selected: false,
        position: {x: 1300, y: 350},
        data: {definition: IsNullNode},
    },
    {
        id: "6",
        type: "bp",
        selected: false,
        position: {x: 1500, y: 350},
        data: {definition: GetExecuteScript},
    },
];

const initialEdges: Edge[] = [];

function ReactFlowImplements() {
    const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes);
    const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges);
    const {getNodes} = useReactFlow();

    const nodeClipboardRef = useRef<Node[] | null>(null);

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

    const getSelectedNodes = () =>
        getNodes().filter((node) => node.selected)

    const onCopy = useCallback(() => {
        const selectedNodes =
            getSelectedNodes().filter((node) => node.selected);
        if (selectedNodes.length === 0)
            return;
        nodeClipboardRef.current = selectedNodes;
    }, []);

    const onPaste = useCallback(() => {
        if (!nodeClipboardRef.current || nodeClipboardRef.current.length === 0)
            return;

        const newNodes = nodeClipboardRef.current.map((node) => ({
            ...node,
            id: `${node.id}-${Date.now()}`,
            position: {
                x: node.position.x + 100,
                y: node.position.y + 100,
            },
            selected: false,
        }));

        setNodes((prevNodes) => [...prevNodes, ...newNodes]);
    }, [setNodes]);

    const onDelete = useCallback(() => {
        const selectedNodes = getSelectedNodes();
        const selectedIds = new Set(selectedNodes.map((n) => n.id));

        setNodes((nodes) =>
            nodes.filter((n) => !selectedIds.has(n.id))
        );

        setEdges((edges) =>
            edges.filter(
                (e) => !selectedIds.has(e.source) && !selectedIds.has(e.target)
            )
        );
    }, [getNodes, setNodes, setEdges]);

    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if ((e.ctrlKey || e.metaKey)) {
                if (e.key === 's') {
                    e.preventDefault();
                    console.log('导出流程图数据：', {nodes, edges,});
                    return;
                }

                if (e.key === 'c') {
                    e.preventDefault();
                    onCopy();
                    return;
                }

                if (e.key === 'v') {
                    e.preventDefault();
                    onPaste();
                    return;
                }

                if (e.key === 'z') {
                    onPaste();
                    return;
                }
            }

            if (e.key === "Delete") {
                onDelete();
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

export default function App() {
    return (
      <ReactFlowProvider>
          <ReactFlowImplements/>
      </ReactFlowProvider>
    );
}