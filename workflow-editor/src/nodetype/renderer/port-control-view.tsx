// src/editor/components/PortControlView.tsx

import {
    useEffect,
    useRef,
    useState,
} from "react";

import {
    PortControl,
} from "../types/port";

import type {
    PortDefinition,
} from "../types/port";

import "../styles/port-control-style.css";

type Props = {
    port: PortDefinition;
    value?: unknown;
    connected?: boolean;
    onChange?(
        value: unknown
    ): void;
};

export function PortControlView({
                                    port,
                                    connected = false,
                                    onChange,
                                }: Props) {

    const [open, setOpen] =
        useState(false);

    const rootRef =
        useRef<HTMLDivElement | null>(
            null,
        );

    /*
     * Blueprint:
     * Pin connected
     * -> hide inline widget
     */
    if (connected)
        return null;

    useEffect(() => {

        function handleClickOutside(
            event: MouseEvent
        ) {

            if (
                !rootRef.current?.contains(
                    event.target as Node,
                )
            ) {
                setOpen(false);
            }
        }

        window.addEventListener(
            "mousedown",
            handleClickOutside,
        );

        return () => {
            window.removeEventListener(
                "mousedown",
                handleClickOutside,
            );
        };

    }, []);

    switch (port.control) {

        case PortControl.Input: {
            const [inputValue, setInputValue] = useState(
                port.value ?? ""
            );

            return (
                <input
                    className="rf-port-input"
                    placeholder={port.placeholder}
                    value={inputValue}
                    onChange={(e) => {
                        setInputValue(e.target.value);
                        port.value = e.target.value;
                    }}
                />
            );
        }

        case PortControl.Checkbox:
            return (
                <label
                    className="
                        rf-port-checkbox
                    "
                >
                    <input
                        type="checkbox"
                        checked={Boolean(port.value)}
                        onChange={(event) => {
                            onChange?.(
                                event.target.checked,
                            );
                        }}
                    />

                    <span/>
                </label>
            );

        case PortControl.Select: {
            let isPlaceholder = false;

            let selectedValue =  port.options?.find(
                (option) => {
                    return (option.value === port.value);
                },
            )?.label ?? undefined;

            if (selectedValue == undefined) {
                isPlaceholder = true;
                selectedValue = port.placeholder
            }

            return (
                <div
                    ref={rootRef}
                    className={`
                        rf-port-select
                        ${open ? "open" : ""}
                    `}
                >
                    <button
                        type="button"
                        className="
                            rf-port-select-button
                        "
                        onClick={() => {
                            setOpen(
                                (prev) => !prev,
                            );
                        }}
                    >
                        <span style={{ color: isPlaceholder ? "#9ca3af" : undefined }}>{selectedValue}</span>

                        <span
                            className="
                                rf-port-select-arrow
                            "
                        >
                            ▼
                        </span>
                    </button>

                    {open && (
                        <div
                            className="
                                rf-port-select-popup
                            "
                        >
                            {port.options?.map(
                                (option) => (
                                    <button
                                        key={
                                            option.value
                                        }
                                        type="button"
                                        className={`
                                            rf-port-select-item
                                            ${
                                            option.value === port.value
                                                ? "active"
                                                : ""
                                        }
                                        `}
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            // onChange?.(option.value,);
                                            setOpen(false,);
                                            port.value = option.value
                                        }}
                                    >
                                        {
                                            option.label
                                        }
                                    </button>
                                ),
                            )}
                        </div>
                    )}
                </div>
            );
        }

        default:
            return null;
    }
}