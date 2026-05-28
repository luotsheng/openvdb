import type {NodeDefinition} from "../types/node";
import {PortControl, PortDataType, type PortDefinition, PortDirection, PortType} from "../types/port";

function createNode(
    title: string,
    type: string,
    inputs: PortDefinition[],
    outputs: PortDefinition[]
): NodeDefinition {
    return {
        title,
        type,
        headerClass: "action-header",
        inputs,
        outputs,
    }
}

export const GetConnectionNode: NodeDefinition = createNode("获取连接对象", "get-connection", [
    {
        id: "in-exec",
        name: "EXEC IN",
        type: PortType.Exec,
        direction: PortDirection.Input
    }
], [
    {
        id: "out-exec-1",
        name: "EXEC OUT",
        type: PortType.Exec,
        direction: PortDirection.Output
    },
    {
        id: "out-value-1",
        name: "",
        type: PortType.Data,
        dataType: PortDataType.Object,
        direction: PortDirection.Output,
        placeholder: "请选择数据库连接",
        control: PortControl.Select,
        options: [
            {label: "本地_MySQL数据库_读写", value: "本地_MySQL数据库_读写"},
            {label: "生产_虹云通达梦_只读", value: "生产_虹云通达梦_只读"},
            {label: "生产_虹云通达梦_读写", value: "生产_虹云通达梦_读写"},
        ],
    }
]);

export const GetDatabaseNode: NodeDefinition = createNode("获取数据库对象", "get-database", [
    {
        id: "in-exec",
        name: "EXEC IN",
        type: PortType.Exec,
        direction: PortDirection.Input
    },
    {
        id: "in-connection-value",
        name: "连接对象",
        type: PortType.Data,
        dataType: PortDataType.Object,
        direction: PortDirection.Input,
    }
], [
    {
        id: "out-exec-1",
        name: "EXEC OUT",
        type: PortType.Exec,
        direction: PortDirection.Output
    },
    {
        id: "out-value-1",
        name: "",
        type: PortType.Data,
        dataType: PortDataType.Object,
        direction: PortDirection.Output,
        placeholder: "数据库名称",
        control: PortControl.Input
    }
]);