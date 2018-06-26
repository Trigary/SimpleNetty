# Sever Reference

## Lifecycle

The following table describes what states specific actions require
and how these actions modify the state.

| Action | State |
| :---: | :---: |
| constructor | N/A -> stopped |
| start | stopped -> started |
| stop | started -> stopped |

## Thread safety

The stop method mustn't be called from an event listener (onXY),
otherwise a deadlock will happen.

The setters for the event listeners (onXY) can be called from any
threads, but due to a lack of synchronization the old event handler
may be called directly after a new one has been set.
