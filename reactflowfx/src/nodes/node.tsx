import { Handle, Position } from 'reactflow';
import type { NodeProps } from 'reactflow';
import type { NodeData } from './types.ts';

export default function MyNode({ data, selected }: NodeProps<NodeData>) {
    return (
        <div
            style={{
                width: 200,
                borderRadius: 8,
                padding: 10,
                background: '#ffffff',
                border: selected ? '2px solid #2563eb' : '1px solid #d1d5db',
                boxShadow: selected ? '0 0 0 3px rgba(37,99,235,0.15)' : 'none',
                color: '#111827',
            }}
        >
            <div style={{ fontSize: 13, fontWeight: 600 }}>
                {data.label}
            </div>

            <div style={{ fontSize: 12, color: '#6b7280', marginTop: 4 }}>
                {data.content}
            </div>

            <Handle
                type="target"
                position={Position.Left}
                style={{ background: '#2563eb' }}
            />
            <Handle
                type="source"
                position={Position.Right}
                style={{ background: '#2563eb' }}
            />
        </div>
    );
}