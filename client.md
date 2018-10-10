# Client Reference

## Lifecycle

The following table describes what states specific actions require
and how these actions modify the state.

| Action | State |
| :---: | :---: |
| constructed | N/A -> uninitialized |
| failed connect | uninitialized -> initialized |
| successful connect | uninitialized -> connected |
| disconnect | connected -> initialized |
| uninitialize | initialized / connected -> uninitialized |

## Thread safety

The uninitialize method mustn't be called from an event (onXY),
otherwise a deadlock will happen.

The setters for the event listeners (onXY) can be called from any thread.
