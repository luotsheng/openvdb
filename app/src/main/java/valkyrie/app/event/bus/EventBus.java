package valkyrie.app.event.bus;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import valkyrie.app.event.workbench.CloseNavigationPaneEvent;
import valkyrie.app.event.workbench.OpenNavigationPaneEvent;
import valkyrie.app.event.workbench.OpenTabEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 事件总线管理器
 *
 * @author Luo Tiansheng
 * @since 2026/3/26
 */
public class EventBus
{
        private static final Logger LOG = LoggerFactory.getLogger(EventBus.class);

        private static final Map<Class<? extends Event>, CopyOnWriteArrayList<EventListener>> eventListeners
                = new ConcurrentHashMap<>();

        /**
         * 订阅事件
         */
        @SafeVarargs
        public static void subscribe(EventListener listener, Class<? extends Event> ...events)
        {
                for (Class<? extends Event> event : events)
                        eventListeners.computeIfAbsent(event, k -> new CopyOnWriteArrayList<>())
                                .add(listener);
        }

        /**
         * 取消订阅
         */
        @SafeVarargs
        public static void unscribe(EventListener listener, Class<? extends Event> ...events)
        {
                for (Class<? extends Event> event : events) {
                        CopyOnWriteArrayList<EventListener> eventListeners = EventBus.eventListeners.get(event);
                        if (eventListeners != null)
                                eventListeners.remove(listener);
                }
        }

        /**
         * 发布事件
         */
        public static void publish(Event event)
        {
                Platform.runLater(() -> {
                        Class<? extends Event> eventClass = event instanceof OpenTabEvent
                                ? OpenTabEvent.class
                                : event.getClass();

                        CopyOnWriteArrayList<EventListener> copyOnWriteEventListeners = eventListeners.get(eventClass);

                        if (copyOnWriteEventListeners == null)
                                return;

                        copyOnWriteEventListeners.forEach(listener -> {
                                try {
                                        if (event.isConsume())
                                                return;
                                        listener.onEvent(event);
                                } catch (Throwable e) {
                                        LOG.error("Consume event failed", e);
                                }
                        });
                });
        }

        public static void openNavigationPane(Object owner, Pane pane)
        {
                publish(new OpenNavigationPaneEvent(owner, pane));
        }

        public static void closeNavigationPane(Object owner)
        {
                publish(new CloseNavigationPaneEvent(owner));
        }

}
