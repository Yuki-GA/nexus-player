# Nexus Runtime Rules

These rules are foundational mandates for the development of the Nexus Runtime. They take precedence over general defaults.

* **Engine stability first**: Prioritize the stability of the core engine above new features.
* **Avoid duplicate systems**: Ensure each responsibility is handled by exactly one system.
* **Avoid manager explosion**: Keep the number of "Manager" classes/systems to a minimum.
* **Avoid fake optimization systems**: Do not implement complex optimization logic that doesn't yield measurable benefits.
* **WebView is authoritative**: The WebView state and environment are the source of truth.
* **Prefer simple deterministic architecture**: Aim for code that is easy to reason about and yields predictable results.
* **Prioritize MV/MZ compatibility**: Ensure the runtime remains compatible with RPG Maker MV and MZ standards.
* **Avoid unnecessary hooks**: Keep the integration points clean and minimal.
* **Avoid architecture drift**: Maintain the established architectural patterns.
* **Delete dead systems after migration**: Clean up legacy systems immediately after they are replaced.
* **Do not stack new systems on top of old systems**: Refactor or replace rather than wrapping old complexity with new complexity.
* **Focus on maintainability and responsiveness**: Code must be easy to maintain and the application must remain responsive to user input.
