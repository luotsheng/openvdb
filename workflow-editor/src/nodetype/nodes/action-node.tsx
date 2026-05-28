import type {NodeDefinition} from "../types/node";
import {PortDataType, type PortDefinition, PortDirection, PortType} from "../types/port";

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
        name: "IN",
        type: PortType.Exec,
        direction: PortDirection.Input
    }
], [
    {
        id: "out-exec-1",
        name: "OUT",
        type: PortType.Exec,
        direction: PortDirection.Output
    },
    {
        id: "out-value-1",
        name: "选择连接",
        type: PortType.Data,
        dataType: PortDataType.Object,
        direction: PortDirection.Output
    }
]);

export const GetDatabaseNode: NodeDefinition = createNode("获取数据库对象", "get-database", [
    {
        id: "in-exec",
        name: "IN",
        type: PortType.Exec,
        direction: PortDirection.Input
    },
    {
        id: "in-connection-value",
        name: "连接对象",
        type: PortType.Data,
        dataType: PortDataType.Object,
        direction: PortDirection.Input
    }
], [
    {
        id: "out-exec-1",
        name: "OUT",
        type: PortType.Exec,
        direction: PortDirection.Output
    },
    {
        id: "out-value-1",
        name: "选择数据库",
        type: PortType.Data,
        dataType: PortDataType.Object,
        direction: PortDirection.Output
    }
]);