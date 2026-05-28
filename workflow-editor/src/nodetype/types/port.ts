export enum PortType {
    Exec = "exec",
    Data = "data"
}

export enum PortDataType {
    Any = "any",
    Int = "int",
    Float = "float",
    Bool = "bool",
    String = "string",
    Object = "object",
    Array = "array",
}

export interface PortDefinition {
    id: string;
    name: string;
    portType: PortType;
    dataType?: PortDataType;
    multiple?: boolean;
    optional?: boolean;
    defaultValue?: unknown;
    control?: string;
}