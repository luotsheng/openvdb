export enum PortDirection {
    Input = "input",
    Output = "output"
}

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

export enum PortControl {
    Select = "select",
    Input = "input",
    Checkbox = "checkbox"
}

export interface PortDefinition {
    id: string;
    name: string;
    direction: PortDirection;
    type: PortType;
    dataType?: PortDataType;
    multiple?: boolean;
    placeholder?: string;
    control?: PortControl;
    value?: string;
    options?: {
        label: string;
        value: string;
    }[];
}