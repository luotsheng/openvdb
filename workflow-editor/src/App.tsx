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
// css
import "reactflow/dist/style.css";

export const initialNodes: Node[] = [
    {
        id: "start-1",
        type: "start",
        position: { x: 100, y: 100 },
        data: {
            title: "开始",
            outputs: [{ id: "out-exec", label: "", type: "exec" }]
        },
    },
    {
        id: "open-conn-1",
        type: "action",
        position: { x: 380, y: 100 },
        data: {
            title: "打开连接",
            inputs: [
                { id: "in-exec", label: "", type: "exec" }
            ],
            outputs: [
                { id: "out-exec", label: "", type: "exec" },
                { id: "out-conn", label: "连接对象", type: "data", dataType: "object" }
            ]
        }
    },
    {
        id: "get-dbname-1",
        type: "action",
        position: { x: 680, y: 80 },
        data: {
            title: "获取数据库名称",
            inputs: [
                { id: "in-exec", label: "", type: "exec" },
                { id: "in-conn", label: "连接对象", type: "data", dataType: "object" }
            ],
            outputs: [
                { id: "out-exec", label: "", type: "exec" },
                { id: "out-name", label: "数据库名", type: "data", dataType: "string" }
            ]
        }
    },
    {
        id: "branch-1",
        type: "branch",
        position: { x: 980, y: 70 },
        data: {
            title: "是否为 OPENSER 数据库？",
            inputs: [
                { id: "in-exec", label: "", type: "exec" },
                { id: "in-name", label: "数据库名", type: "data", dataType: "string" }
            ],
            outputs: [
                { id: "out-true", label: "是", type: "exec" },
                { id: "out-false", label: "否", type: "exec" }
            ],
            properties: { condition: "dbName === 'OPENSER'" }
        }
    },
    {
        id: "query-1",
        type: "action",
        position: { x: 1280, y: 20 },
        data: {
            title: "执行查询",
            inputs: [
                { id: "in-exec", label: "", type: "exec" }
            ],
            outputs: [
                { id: "out-exec", label: "", type: "exec" },
                { id: "out-data", label: "查询结果", type: "data", dataType: "array" }
            ]
        }
    },
    {
        id: "export-excel-1",
        type: "action",
        position: { x: 1580, y: 20 },
        data: {
            title: "导出为 Excel",
            inputs: [
                { id: "in-exec", label: "", type: "exec" },
                { id: "in-data", label: "数据", type: "data", dataType: "array" }
            ],
            outputs: [
                { id: "out-exec", label: "", type: "exec" }
            ]
        }
    },
    {
        id: "end-1",
        type: "end",
        position: { x: 1880, y: 100 },
        data: {
            title: "结束",
            inputs: [
                { id: "in-exec", label: "", type: "exec" }
            ]
        }
    }
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