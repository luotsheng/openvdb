import * as RadixDialog from "@radix-ui/react-dialog";
import { useRef, useState, type MouseEvent as ReactMouseEvent, type ReactNode } from "react";

interface DialogProps {
  title: ReactNode;
  children: ReactNode;
  role?: "dialog" | "alertdialog";
  className?: string;
  /** 传入后支持 Esc 关闭 */
  onClose?: () => void;
}

const DRAG_MARGIN = 60;

/**
 * 模态框（Radix Dialog）：焦点陷阱、Esc 关闭、滚动锁、aria 都交给组件，
 * 这里只补两件事：标题栏拖动（transform 实现）与不响应背景点击关闭。
 */
export function Dialog({ title, children, role = "dialog", className = "", onClose }: DialogProps) {
  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const contentRef = useRef<HTMLDivElement | null>(null);
  const dragRef = useRef<{ px: number; py: number; ox: number; oy: number; maxX: number; maxY: number } | null>(null);

  function startDrag(event: ReactMouseEvent<HTMLDivElement>) {
    /* 标题里的按钮 / 输入框不触发拖动 */
    if ((event.target as HTMLElement).closest("button, input, select, a"))
      return;

    const rect = contentRef.current?.getBoundingClientRect();

    if (!rect)
      return;

    event.preventDefault();
    dragRef.current = {
      px: event.clientX,
      py: event.clientY,
      ox: offset.x,
      oy: offset.y,
      maxX: Math.max(0, window.innerWidth / 2 + rect.width / 2 - DRAG_MARGIN),
      maxY: Math.max(0, window.innerHeight / 2 + rect.height / 2 - DRAG_MARGIN)
    };

    const clamp = (value: number, limit: number) => Math.max(-limit, Math.min(limit, value));

    const onMove = (moveEvent: MouseEvent) => {
      const drag = dragRef.current;

      if (!drag)
        return;

      setOffset({
        x: clamp(drag.ox + moveEvent.clientX - drag.px, drag.maxX),
        y: clamp(drag.oy + moveEvent.clientY - drag.py, drag.maxY)
      });
    };

    const onUp = () => {
      dragRef.current = null;
      document.body.classList.remove("is-dragging");
      window.removeEventListener("mousemove", onMove);
      window.removeEventListener("mouseup", onUp);
    };

    document.body.classList.add("is-dragging");
    window.addEventListener("mousemove", onMove);
    window.addEventListener("mouseup", onUp);
  }

  return (
    <RadixDialog.Root open onOpenChange={open => { if (!open) onClose?.(); }}>
      <RadixDialog.Portal>
        <RadixDialog.Overlay className="modal-backdrop">
          <RadixDialog.Content
            ref={contentRef}
            className={`modal${className ? ` ${className}` : ""}`}
            style={{ transform: `translate(${offset.x}px, ${offset.y}px)` }}
            role={role}
            aria-describedby={undefined}
            /* 不允许点背景关闭：避免误点丢失表单内容 */
            onPointerDownOutside={event => event.preventDefault()}
            onInteractOutside={event => event.preventDefault()}
          >
            <RadixDialog.Title asChild>
              <div className="modal-title" onMouseDown={startDrag}>{title}</div>
            </RadixDialog.Title>

            {children}
          </RadixDialog.Content>
        </RadixDialog.Overlay>
      </RadixDialog.Portal>
    </RadixDialog.Root>
  );
}
